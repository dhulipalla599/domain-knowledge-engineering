package com.dke.retail.order_management_and_fulfillment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.retail.order_management_and_fulfillment.domain.FulfillmentNode;
import com.dke.retail.order_management_and_fulfillment.domain.InventoryPosition;
import com.dke.retail.order_management_and_fulfillment.service.SourcingService.LineRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SourcingServiceTest {

    private final SourcingService sourcing = new SourcingService();

    private final List<FulfillmentNode> nodes = List.of(
            new FulfillmentNode("DC-EAST", "Harrisburg DC", "DC", "EAST", 10, true),
            new FulfillmentNode("DC-WEST", "Reno DC", "DC", "WEST", 10, true),
            new FulfillmentNode("STORE-114", "Brooklyn store", "STORE", "EAST", 20, true),
            new FulfillmentNode("STORE-301", "Denver store", "STORE", "WEST", 20, false));

    private final List<InventoryPosition> stock = List.of(
            new InventoryPosition("DC-EAST", "KTL-200", 40, 0),
            new InventoryPosition("DC-EAST", "MUG-SET4", 25, 0),
            new InventoryPosition("DC-EAST", "TOAST-2", 6, 0),
            new InventoryPosition("DC-WEST", "KTL-200", 30, 0),
            new InventoryPosition("DC-WEST", "FRY-28", 12, 0),
            new InventoryPosition("STORE-114", "KTL-200", 3, 0),
            new InventoryPosition("STORE-114", "FRY-28", 2, 1),
            new InventoryPosition("STORE-301", "DOV-LTD", 3, 0));

    @Test
    void sameRegionDcWinsWhenItCanShipEverything() {
        Map<String, String> plan = sourcing.source(
                List.of(new LineRequest("KTL-200", 2), new LineRequest("MUG-SET4", 1)), "EAST", nodes, stock);
        assertThat(plan).containsExactly(Map.entry("KTL-200", "DC-EAST"), Map.entry("MUG-SET4", "DC-EAST"));
    }

    @Test
    void oneNodeThatShipsTheWholeOrderBeatsAHigherPriorityNodeThatWouldSplitIt() {
        Map<String, String> plan = sourcing.source(
                List.of(new LineRequest("KTL-200", 2), new LineRequest("FRY-28", 1)), "EAST", nodes, stock);
        assertThat(plan).containsOnly(Map.entry("KTL-200", "STORE-114"), Map.entry("FRY-28", "STORE-114"));
    }

    @Test
    void reservedUnitsAreNotAvailableSoTheOrderIsSplitAcrossNodes() {
        // STORE-114 has 2 skillets on hand but 1 is reserved: only 1 is available-to-promise.
        Map<String, String> plan = sourcing.source(
                List.of(new LineRequest("TOAST-2", 2), new LineRequest("FRY-28", 2)), "EAST", nodes, stock);
        assertThat(plan).containsOnly(Map.entry("TOAST-2", "DC-EAST"), Map.entry("FRY-28", "DC-WEST"));
    }

    @Test
    void inactiveNodesAreIgnoredAndShortageIsRejected() {
        assertThatThrownBy(() -> sourcing.source(List.of(new LineRequest("DOV-LTD", 1)), "WEST", nodes, stock))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No fulfillment node can supply 1 x DOV-LTD")
                .extracting("rule").isEqualTo("INSUFFICIENT_STOCK");
    }
}
