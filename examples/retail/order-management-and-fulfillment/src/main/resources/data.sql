-- Fictional catalogue, nodes and stock for "Larkspur Home", an omnichannel homeware retailer.
INSERT INTO product (sku, name, unit_price, max_per_order) VALUES ('KTL-200', 'Electric kettle 1.7L', 39.99, 5);
INSERT INTO product (sku, name, unit_price, max_per_order) VALUES ('MUG-SET4', 'Stoneware mug set of 4', 24.50, 10);
INSERT INTO product (sku, name, unit_price, max_per_order) VALUES ('FRY-28', 'Cast-iron skillet 28cm', 54.00, 3);
INSERT INTO product (sku, name, unit_price, max_per_order) VALUES ('TOAST-2', 'Two-slice toaster (promo)', 32.00, 2);
INSERT INTO product (sku, name, unit_price, max_per_order) VALUES ('DOV-LTD', 'Limited-edition Dutch oven', 189.00, 2);

INSERT INTO fulfillment_node (code, name, node_type, region, priority, active) VALUES ('DC-EAST', 'Harrisburg distribution centre', 'DC', 'EAST', 10, TRUE);
INSERT INTO fulfillment_node (code, name, node_type, region, priority, active) VALUES ('DC-WEST', 'Reno distribution centre', 'DC', 'WEST', 10, TRUE);
INSERT INTO fulfillment_node (code, name, node_type, region, priority, active) VALUES ('STORE-114', 'Brooklyn store', 'STORE', 'EAST', 20, TRUE);
INSERT INTO fulfillment_node (code, name, node_type, region, priority, active) VALUES ('STORE-207', 'Seattle store', 'STORE', 'WEST', 20, TRUE);
INSERT INTO fulfillment_node (code, name, node_type, region, priority, active) VALUES ('STORE-301', 'Denver store (stocktake, paused)', 'STORE', 'WEST', 20, FALSE);

INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('DC-EAST', 'KTL-200', 40, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('DC-EAST', 'MUG-SET4', 25, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('DC-EAST', 'TOAST-2', 6, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('DC-WEST', 'KTL-200', 30, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('DC-WEST', 'MUG-SET4', 18, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('DC-WEST', 'FRY-28', 12, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('STORE-114', 'KTL-200', 3, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('STORE-114', 'MUG-SET4', 4, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('STORE-114', 'FRY-28', 2, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('STORE-207', 'FRY-28', 1, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('STORE-207', 'TOAST-2', 2, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('STORE-207', 'DOV-LTD', 1, 0, 0);
INSERT INTO inventory_position (node_code, sku, on_hand, reserved, version) VALUES ('STORE-301', 'DOV-LTD', 3, 0, 0);

-- One order shipped yesterday, so GET /api/orders/R100001 returns something straight away.
INSERT INTO customer_order (order_number, idempotency_key, customer_id, ship_to_region, status, total_amount, currency,
                            payment_authorization_id, placed_at, version)
VALUES ('R100001', 'seed-0001', 'CUST-2087', 'WEST', 'SHIPPED', 49.00, 'USD', 'AUTH-R100001', TIMESTAMP '2026-11-11 16:05:00', 0);
INSERT INTO order_line (order_id, sku, quantity, unit_price, fulfillment_node, tracking_number)
VALUES (1, 'MUG-SET4', 2, 24.50, 'DC-WEST', 'TRK-DC-WEST-R100001');
