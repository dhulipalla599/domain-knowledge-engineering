package com.dke.retail.store_replenishment.service;

import com.dke.retail.store_replenishment.domain.DomainException;
import com.dke.retail.store_replenishment.domain.OrderStatus;
import com.dke.retail.store_replenishment.domain.ReplenishmentEvent;
import com.dke.retail.store_replenishment.domain.ReplenishmentOrder;
import com.dke.retail.store_replenishment.domain.StoreSku;
import com.dke.retail.store_replenishment.integration.DistributionCenterGateway;
import com.dke.retail.store_replenishment.repository.ReplenishmentOrderRepository;
import com.dke.retail.store_replenishment.repository.StoreSkuRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplenishmentService {

    /** created = how many orders this call added; a re-run for the same date returns the same orders. */
    public record RunResult(String storeId, LocalDate runDate, int created, List<ReplenishmentOrder> orders) {
    }

    private final StoreSkuRepository skus;
    private final ReplenishmentOrderRepository orders;
    private final DistributionCenterGateway dc;
    private final ApplicationEventPublisher events;

    public ReplenishmentService(StoreSkuRepository skus, ReplenishmentOrderRepository orders,
                                DistributionCenterGateway dc, ApplicationEventPublisher events) {
        this.skus = skus;
        this.orders = orders;
        this.dc = dc;
        this.events = events;
    }

    /** Nightly run for one store: propose an order for every SKU at or below its reorder point. */
    @Transactional
    public RunResult runReplenishment(String storeId, LocalDate runDate) {
        List<StoreSku> storeSkus = skus.findByStoreIdOrderBySku(storeId);
        if (storeSkus.isEmpty()) {
            throw new DomainException.NotFound("Store", storeId);
        }
        List<ReplenishmentOrder> result = new ArrayList<>();
        int created = 0;
        for (StoreSku item : storeSkus) {
            var existing = orders.findByStoreIdAndSkuAndRunDate(storeId, item.getSku(), runDate);
            if (existing.isPresent()) {
                result.add(existing.get()); // idempotent re-run: never a second order for the same day
                continue;
            }
            if (orders.existsByStoreIdAndSkuAndStatus(storeId, item.getSku(), OrderStatus.PROPOSED)) {
                continue; // an unreleased order already covers this SKU
            }
            int quantity = item.suggestedQuantity();
            if (quantity > 0) {
                result.add(propose(new ReplenishmentOrder(storeId, item.getSku(), quantity, "AUTO", runDate)));
                created++;
            }
        }
        return new RunResult(storeId, runDate, created, result);
    }

    /** A store manager keys an order by hand, e.g. for a local event the forecast cannot see. */
    @Transactional
    public ReplenishmentOrder placeManualOrder(String storeId, String sku, int quantity) {
        StoreSku item = storeSku(storeId, sku);
        item.checkOrderQuantity(quantity); // throws CasePackViolation or ShelfCapacityExceeded
        return propose(new ReplenishmentOrder(storeId, sku, quantity, "MANUAL", null));
    }

    @Transactional
    public ReplenishmentOrder release(String orderId) {
        ReplenishmentOrder order = find(orderId);
        order.release(dc.submit(order));
        storeSku(order.getStoreId(), order.getSku()).addOnOrder(order.getQuantity());
        events.publishEvent(new ReplenishmentEvent.OrderReleased(order.getId(), order.getStoreId(),
                order.getSku(), order.getQuantity(), order.getDcOrderRef()));
        return order;
    }

    @Transactional
    public ReplenishmentOrder receive(String orderId, int receivedQuantity) {
        ReplenishmentOrder order = find(orderId);
        order.receive(receivedQuantity);
        storeSku(order.getStoreId(), order.getSku()).receive(order.getQuantity(), receivedQuantity);
        events.publishEvent(new ReplenishmentEvent.OrderReceived(order.getId(), order.getStoreId(),
                order.getSku(), order.getQuantity(), receivedQuantity));
        return order;
    }

    @Transactional
    public ReplenishmentOrder cancel(String orderId) {
        ReplenishmentOrder order = find(orderId);
        order.cancel();
        return order;
    }

    @Transactional(readOnly = true)
    public List<ReplenishmentOrder> ordersFor(String storeId) {
        return orders.findByStoreIdOrderByCreatedAt(storeId);
    }

    @Transactional(readOnly = true)
    public StoreSku storeSku(String storeId, String sku) {
        return skus.findByStoreIdAndSku(storeId, sku)
                .orElseThrow(() -> new DomainException.NotFound("Store SKU", storeId + "/" + sku));
    }

    private ReplenishmentOrder propose(ReplenishmentOrder order) {
        ReplenishmentOrder saved = orders.save(order);
        events.publishEvent(new ReplenishmentEvent.OrderProposed(saved.getId(), saved.getStoreId(),
                saved.getSku(), saved.getQuantity(), saved.getSource()));
        return saved;
    }

    private ReplenishmentOrder find(String id) {
        return orders.findById(id).orElseThrow(() -> new DomainException.NotFound("Order", id));
    }
}
