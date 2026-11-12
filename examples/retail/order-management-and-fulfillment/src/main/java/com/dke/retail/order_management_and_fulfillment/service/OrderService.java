package com.dke.retail.order_management_and_fulfillment.service;

import com.dke.retail.order_management_and_fulfillment.domain.CustomerOrder;
import com.dke.retail.order_management_and_fulfillment.domain.InventoryPosition;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderAllocated;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderCancelled;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderCreated;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderDelivered;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderShipped;
import com.dke.retail.order_management_and_fulfillment.domain.OrderLine;
import com.dke.retail.order_management_and_fulfillment.domain.Product;
import com.dke.retail.order_management_and_fulfillment.integration.CarrierGateway;
import com.dke.retail.order_management_and_fulfillment.integration.PaymentGateway;
import com.dke.retail.order_management_and_fulfillment.repository.CustomerOrderRepository;
import com.dke.retail.order_management_and_fulfillment.repository.FulfillmentNodeRepository;
import com.dke.retail.order_management_and_fulfillment.repository.InventoryPositionRepository;
import com.dke.retail.order_management_and_fulfillment.repository.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order capture, allocation, shipment and cancellation: the business rules live here and in the domain. */
@Service
public class OrderService {

    public record PlaceOrderCommand(String customerId, String shipToRegion, String paymentToken,
                                    List<SourcingService.LineRequest> lines) {
    }

    public record PlaceOrderResult(CustomerOrder order, boolean created) {
    }

    private final CustomerOrderRepository orders;
    private final ProductRepository products;
    private final FulfillmentNodeRepository nodes;
    private final InventoryPositionRepository inventory;
    private final SourcingService sourcing;
    private final PaymentGateway payments;
    private final CarrierGateway carrier;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderService(CustomerOrderRepository orders, ProductRepository products, FulfillmentNodeRepository nodes,
                        InventoryPositionRepository inventory, SourcingService sourcing, PaymentGateway payments,
                        CarrierGateway carrier, ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.nodes = nodes;
        this.inventory = inventory;
        this.sourcing = sourcing;
        this.payments = payments;
        this.carrier = carrier;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public PlaceOrderResult placeOrder(String idempotencyKey, PlaceOrderCommand cmd) {
        // Rule: a retried checkout returns the original order instead of charging twice.
        var existing = orders.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new PlaceOrderResult(existing.get(), false);
        }

        // Rule: prices come from the catalogue, never the client; purchase limits apply per SKU.
        List<String> skus = cmd.lines().stream().map(SourcingService.LineRequest::sku).toList();
        Map<String, Product> catalogue = products.findAllById(skus).stream()
                .collect(Collectors.toMap(Product::getSku, Function.identity()));
        Set<String> seen = new HashSet<>();
        for (SourcingService.LineRequest line : cmd.lines()) {
            if (!seen.add(line.sku())) {
                throw new BusinessRuleException("DUPLICATE_SKU", "SKU %s appears twice".formatted(line.sku()),
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            Product product = catalogue.get(line.sku());
            if (product == null) {
                throw new BusinessRuleException("UNKNOWN_SKU", "SKU %s is not sold".formatted(line.sku()),
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (line.quantity() > product.getMaxPerOrder()) {
                throw new BusinessRuleException("PURCHASE_LIMIT",
                        "At most %d x %s per order".formatted(product.getMaxPerOrder(), line.sku()),
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // Rule: only promise what a node can actually ship (available-to-promise).
        List<InventoryPosition> positions = inventory.findBySkuIn(skus);
        Map<String, String> plan = sourcing.source(cmd.lines(), cmd.shipToRegion(), nodes.findAll(), positions);

        Instant now = Instant.now(clock);
        CustomerOrder order = CustomerOrder.create(cmd.customerId(), idempotencyKey, cmd.shipToRegion(), "USD", now);
        cmd.lines().forEach(l -> order.addLine(l.sku(), l.quantity(), catalogue.get(l.sku()).getUnitPrice()));
        orders.save(order);
        order.assignOrderNumber("R" + (100000 + order.getId()));

        // Rule: authorise payment before reserving stock, so a declined card never holds inventory.
        PaymentGateway.Authorization auth = payments.authorize(cmd.paymentToken(), order.getTotalAmount(),
                order.getCurrency(), order.getOrderNumber());
        if (!auth.approved()) {
            throw new BusinessRuleException("PAYMENT_DECLINED", "Payment was declined: " + auth.declineReason(),
                    HttpStatus.PAYMENT_REQUIRED);
        }
        order.recordAuthorization(auth.authorizationId());

        for (OrderLine line : order.getLines()) {
            String nodeCode = plan.get(line.getSku());
            position(positions, nodeCode, line.getSku()).reserve(line.getQuantity());
            line.assignNode(nodeCode);
        }
        order.markAllocated();

        events.publishEvent(new OrderCreated(order.getOrderNumber(), order.getCustomerId(),
                order.getTotalAmount(), order.getCurrency(), now));
        events.publishEvent(new OrderAllocated(order.getOrderNumber(), plan, now));
        return new PlaceOrderResult(order, true);
    }

    @Transactional
    public CustomerOrder ship(String orderNumber) {
        CustomerOrder order = get(orderNumber);
        order.markShipped();
        List<InventoryPosition> positions = inventory.findBySkuIn(
                order.getLines().stream().map(OrderLine::getSku).toList());
        Map<String, String> trackingByNode = new TreeMap<>();
        for (OrderLine line : order.getLines()) {
            String tracking = trackingByNode.computeIfAbsent(line.getFulfillmentNode(),
                    node -> carrier.createLabel(node, orderNumber));
            line.recordShipment(tracking);
            position(positions, line.getFulfillmentNode(), line.getSku()).consume(line.getQuantity());
        }
        // Card schemes generally expect capture when goods ship, not when the order is placed.
        payments.capture(order.getPaymentAuthorizationId(), order.getTotalAmount());
        events.publishEvent(new OrderShipped(orderNumber, new ArrayList<>(trackingByNode.values()), Instant.now(clock)));
        return order;
    }

    @Transactional
    public CustomerOrder markDelivered(String orderNumber) {
        CustomerOrder order = get(orderNumber);
        order.markDelivered();
        events.publishEvent(new OrderDelivered(orderNumber, Instant.now(clock)));
        return order;
    }

    @Transactional
    public CustomerOrder cancel(String orderNumber, String reason) {
        CustomerOrder order = get(orderNumber);
        order.cancel();
        List<InventoryPosition> positions = inventory.findBySkuIn(
                order.getLines().stream().map(OrderLine::getSku).toList());
        order.getLines().stream()
                .filter(l -> l.getFulfillmentNode() != null)
                .forEach(l -> position(positions, l.getFulfillmentNode(), l.getSku()).release(l.getQuantity()));
        payments.voidAuthorization(order.getPaymentAuthorizationId());
        events.publishEvent(new OrderCancelled(orderNumber, reason, Instant.now(clock)));
        return order;
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(String orderNumber) {
        return orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("Order " + orderNumber + " not found"));
    }

    @Transactional(readOnly = true)
    public List<InventoryPosition> inventoryFor(String sku) {
        return inventory.findBySkuOrderByNodeCode(sku);
    }

    private static InventoryPosition position(List<InventoryPosition> positions, String nodeCode, String sku) {
        return positions.stream()
                .filter(p -> p.getNodeCode().equals(nodeCode) && p.getSku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No inventory record for %s at %s".formatted(sku, nodeCode)));
    }
}
