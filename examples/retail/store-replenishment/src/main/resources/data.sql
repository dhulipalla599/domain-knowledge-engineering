-- Store S-104 (Maple Grove) and S-207 (Riverside). Columns follow StoreSku.
INSERT INTO store_sku (store_id, sku, on_hand, on_order, reorder_point, order_up_to, case_pack, shelf_capacity, version) VALUES
  ('S-104', 'MILK-2L',     18,  0, 24, 60, 12, 72, 0),  -- below reorder point: order 48 (4 cases)
  ('S-104', 'BREAD-WW',    30,  0, 20, 50, 10, 60, 0),  -- healthy: no order
  ('S-104', 'EGGS-12',      5,  0, 12, 36, 15, 40, 0),  -- 45 would overflow the shelf: order 30 (2 cases)
  ('S-104', 'COFFEE-500G',  8, 12, 18, 36,  6, 42, 0),  -- on-order stock lifts position to 20: no order
  ('S-207', 'MILK-2L',     40,  0, 24, 60, 12, 72, 0),
  ('S-207', 'BANANA-KG',    6,  0, 15, 45,  9, 54, 0);  -- order 45 (5 cases)
