package com.dke.retail.order_management_and_fulfillment.service;

import com.dke.retail.order_management_and_fulfillment.domain.FulfillmentNode;
import com.dke.retail.order_management_and_fulfillment.domain.InventoryPosition;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Decides which fulfillment node ships each order line. Pure logic: no I/O, easy to unit test. */
@Component
public class SourcingService {

    public record LineRequest(String sku, int quantity) {
    }

    /**
     * Returns SKU -> node code. Nodes in the ship-to region come first, then by priority.
     * One node that can ship the whole order wins (one parcel, one shipping cost); otherwise
     * each line goes to the best node that holds all of it. A line is never split across nodes.
     */
    public Map<String, String> source(List<LineRequest> lines, String shipToRegion,
                                      List<FulfillmentNode> nodes, List<InventoryPosition> positions) {
        Map<String, Integer> available = new HashMap<>();
        positions.forEach(p -> available.merge(p.getNodeCode() + "|" + p.getSku(), p.available(), Integer::sum));

        List<FulfillmentNode> ranked = nodes.stream()
                .filter(FulfillmentNode::isActive)
                .sorted(Comparator.comparing((FulfillmentNode n) -> !n.getRegion().equals(shipToRegion))
                        .thenComparingInt(FulfillmentNode::getPriority)
                        .thenComparing(FulfillmentNode::getCode))
                .toList();

        for (FulfillmentNode node : ranked) {
            boolean shipsEverything = lines.stream()
                    .allMatch(l -> available.getOrDefault(node.getCode() + "|" + l.sku(), 0) >= l.quantity());
            if (shipsEverything) {
                Map<String, String> plan = new LinkedHashMap<>();
                lines.forEach(l -> plan.put(l.sku(), node.getCode()));
                return plan;
            }
        }

        Map<String, String> plan = new LinkedHashMap<>();
        for (LineRequest line : lines) {
            String nodeCode = ranked.stream()
                    .filter(n -> available.getOrDefault(n.getCode() + "|" + line.sku(), 0) >= line.quantity())
                    .map(FulfillmentNode::getCode)
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("INSUFFICIENT_STOCK",
                            "No fulfillment node can supply %d x %s".formatted(line.quantity(), line.sku()),
                            HttpStatus.CONFLICT));
            plan.put(line.sku(), nodeCode);
        }
        return plan;
    }
}
