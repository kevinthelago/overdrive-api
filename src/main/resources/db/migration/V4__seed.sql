-- V4: Realistic starter distribution network seed
-- Versioned Flyway migration — runs exactly once; inherently idempotent.
-- Data represents a mid-market consumer-goods distributor's typical catalog.

-- ─── ZIP Centroids (representative sample for routing engine) ─────────────────

INSERT INTO zip_centroid (zip, lat, lng, city, state, region) VALUES
-- Northeast
('10001', 40.7484,  -73.9967, 'New York',       'NY', 'NORTHEAST'),
('02101', 42.3584,  -71.0598, 'Boston',          'MA', 'NORTHEAST'),
('19101', 39.9526,  -75.1652, 'Philadelphia',    'PA', 'NORTHEAST'),
('10301', 40.6298,  -74.0942, 'Staten Island',   'NY', 'NORTHEAST'),
('06101', 41.7637,  -72.6851, 'Hartford',        'CT', 'NORTHEAST'),
-- Southeast
('30301', 33.7490,  -84.3880, 'Atlanta',         'GA', 'SOUTHEAST'),
('33101', 25.7617,  -80.1918, 'Miami',           'FL', 'SOUTHEAST'),
('27601', 35.7796,  -78.6382, 'Raleigh',         'NC', 'SOUTHEAST'),
('28201', 35.2271,  -80.8431, 'Charlotte',       'NC', 'SOUTHEAST'),
('29201', 34.0007,  -81.0348, 'Columbia',        'SC', 'SOUTHEAST'),
-- Midwest
('60601', 41.8827,  -87.6233, 'Chicago',         'IL', 'MIDWEST'),
('43201', 39.9612,  -82.9988, 'Columbus',        'OH', 'MIDWEST'),
('48201', 42.3314,  -83.0458, 'Detroit',         'MI', 'MIDWEST'),
('55401', 44.9778,  -93.2650, 'Minneapolis',     'MN', 'MIDWEST'),
('64101', 39.0997,  -94.5786, 'Kansas City',     'MO', 'MIDWEST'),
-- Southwest
('75201', 32.7767,  -96.7970, 'Dallas',          'TX', 'SOUTHWEST'),
('77001', 29.7604,  -95.3698, 'Houston',         'TX', 'SOUTHWEST'),
('85001', 33.4484, -112.0740, 'Phoenix',         'AZ', 'SOUTHWEST'),
('73101', 35.4676,  -97.5164, 'Oklahoma City',   'OK', 'SOUTHWEST'),
('78201', 29.4241,  -98.4936, 'San Antonio',     'TX', 'SOUTHWEST'),
-- West
('90001', 34.0522, -118.2437, 'Los Angeles',     'CA', 'WEST'),
('94101', 37.7749, -122.4194, 'San Francisco',   'CA', 'WEST'),
('98101', 47.6062, -122.3321, 'Seattle',         'WA', 'WEST'),
('80201', 39.7392, -104.9903, 'Denver',          'CO', 'WEST'),
('97201', 45.5051, -122.6750, 'Portland',        'OR', 'WEST');


-- ─── Category Rates ───────────────────────────────────────────────────────────

INSERT INTO category_rate (category, payment_rate, return_rate) VALUES
('ELECTRONICS',     0.0150, 0.1500),   -- net-30 ~1.5% discount; high returns
('HEALTH_BEAUTY',   0.0100, 0.0800),
('HARDWARE_TOOLS',  0.0100, 0.0500),
('OFFICE_SUPPLIES', 0.0075, 0.0400),
('HOME_GOODS',      0.0100, 0.0600),
('SPORTING_GOODS',  0.0125, 0.0700),
('APPAREL',         0.0125, 0.2500),   -- apparel has highest return rate
('AUTOMOTIVE',      0.0100, 0.0450);


-- ─── Suppliers (≥5) ──────────────────────────────────────────────────────────

INSERT INTO supplier (id, name, moq, lead_time_days, cost_amount, cost_currency,
                      origin_zip, origin_lat, origin_lng, volume_discount_pct, reliability_score)
VALUES
('a1000000-0000-0000-0000-000000000001', 'Pacific Rim Imports',        250, 45, NULL, 'USD',
    '90001', 34.0522, -118.2437, 0.0800, 0.88),
('a1000000-0000-0000-0000-000000000002', 'Great Plains Wholesale',     100, 14, NULL, 'USD',
    '64101', 39.0997,  -94.5786, 0.0500, 0.95),
('a1000000-0000-0000-0000-000000000003', 'Eastern Manufacturing Co',   500, 60, NULL, 'USD',
    '10001', 40.7484,  -73.9967, 0.1000, 0.82),
('a1000000-0000-0000-0000-000000000004', 'Southern Distribution LLC',   50,  7, NULL, 'USD',
    '30301', 33.7490,  -84.3880, 0.0300, 0.97),
('a1000000-0000-0000-0000-000000000005', 'Midwest Direct Supplies',    200, 21, NULL, 'USD',
    '60601', 41.8827,  -87.6233, 0.0600, 0.92),
('a1000000-0000-0000-0000-000000000006', 'Pacific Coast Logistics',    100, 30, NULL, 'USD',
    '98101', 47.6062, -122.3321, 0.0700, 0.90);


-- ─── Products (≥20 across ≥5 categories) ──────────────────────────────────────

INSERT INTO product (id, sku, name, category,
                     weight_lbs, length_in, width_in, height_in,
                     hazardous, fragile, temperature_sensitive, stackable, pallet_qty,
                     cost_amount, cost_currency, msrp_amount, msrp_currency,
                     market_size, order_frequency, category_growth, logistics_complexity)
VALUES
-- ELECTRONICS (high value, fragile, varies in weight)
('b1000000-0000-0000-0000-000000000001', 'ELEC-BT-001', 'ProSound Bluetooth Speaker',   'ELECTRONICS',
    2.50, 10.0, 6.0, 5.0,  FALSE, TRUE,  FALSE, FALSE, 48,
    28.00, 'USD', 79.99, 'USD',  450000, 2.4, 0.08, 5.5),

('b1000000-0000-0000-0000-000000000002', 'ELEC-HD-001', '4K Portable Monitor 15"',     'ELECTRONICS',
    4.20, 15.5, 10.0, 2.5, FALSE, TRUE,  FALSE, FALSE, 24,
    75.00, 'USD', 199.99, 'USD', 220000, 1.8, 0.12, 6.5),

('b1000000-0000-0000-0000-000000000003', 'ELEC-CHG-001', 'GaN 100W USB-C Charger',    'ELECTRONICS',
    0.55,  4.0,  3.0, 2.0, FALSE, FALSE, FALSE, TRUE,  200,
    12.00, 'USD', 39.99, 'USD',  900000, 3.5, 0.18, 2.5),

('b1000000-0000-0000-0000-000000000004', 'ELEC-CB-001', 'True Wireless Earbuds Pro',   'ELECTRONICS',
    0.40,  5.0,  3.5, 2.5, FALSE, FALSE, FALSE, TRUE,  240,
    18.00, 'USD', 59.99, 'USD', 1200000, 4.2, 0.15, 3.0),

-- HEALTH_BEAUTY
('b1000000-0000-0000-0000-000000000005', 'HB-VIT-001', 'Vitamin D3 + K2 Softgels 90ct','HEALTH_BEAUTY',
    0.60,  4.0,  3.0, 4.5, FALSE, FALSE, FALSE, TRUE,  360,
     4.25, 'USD', 18.99, 'USD', 3400000, 6.0, 0.10, 1.5),

('b1000000-0000-0000-0000-000000000006', 'HB-PRO-001', 'Marine Collagen Peptides 500g','HEALTH_BEAUTY',
    1.30,  6.0,  4.0, 5.0, FALSE, FALSE, FALSE, TRUE,  180,
     7.50, 'USD', 34.99, 'USD', 1800000, 4.8, 0.14, 2.0),

('b1000000-0000-0000-0000-000000000007', 'HB-SK-001', 'Retinol Night Cream 50ml',      'HEALTH_BEAUTY',
    0.25,  3.0,  2.0, 3.5, FALSE, TRUE,  FALSE, TRUE,  600,
     6.00, 'USD', 28.99, 'USD', 2200000, 5.5, 0.09, 2.5),

-- HARDWARE_TOOLS
('b1000000-0000-0000-0000-000000000008', 'HW-DRV-001', '20V Cordless Drill/Driver Kit','HARDWARE_TOOLS',
    6.80, 14.0, 10.0, 5.0, FALSE, FALSE, FALSE, FALSE, 12,
    38.00, 'USD', 99.99, 'USD',  780000, 1.5, 0.06, 5.0),

('b1000000-0000-0000-0000-000000000009', 'HW-TPC-001', 'Professional Tape Measure 25ft','HARDWARE_TOOLS',
    0.80,  5.0,  5.0, 3.0, FALSE, FALSE, FALSE, TRUE,  144,
     4.00, 'USD', 14.99, 'USD', 2000000, 3.0, 0.04, 1.5),

('b1000000-0000-0000-0000-000000000010', 'HW-SAW-001', '7-1/4" Circular Saw 5200RPM', 'HARDWARE_TOOLS',
    9.50, 20.0, 10.0, 9.0, FALSE, FALSE, FALSE, FALSE,  8,
    55.00, 'USD', 139.99, 'USD', 480000, 1.2, 0.05, 6.5),

-- OFFICE_SUPPLIES
('b1000000-0000-0000-0000-000000000011', 'OF-PAPER-001', 'Copy Paper 20lb 500-Sheet Ream','OFFICE_SUPPLIES',
    5.00, 11.5,  9.0, 2.5, FALSE, FALSE, FALSE, TRUE,   40,
     2.50, 'USD',  9.99, 'USD', 5000000, 8.0, 0.01, 3.0),

('b1000000-0000-0000-0000-000000000012', 'OF-INK-001', 'Black Ballpoint Pens 36-Pack',  'OFFICE_SUPPLIES',
    0.90,  8.5,  5.0, 3.0, FALSE, FALSE, FALSE, TRUE,  300,
     2.80, 'USD',  9.99, 'USD', 3500000, 7.5, 0.02, 1.0),

('b1000000-0000-0000-0000-000000000013', 'OF-DESK-001', 'Mesh Desk Organizer 5-Tier',  'OFFICE_SUPPLIES',
    3.40, 12.0,  9.0, 14.0,FALSE, FALSE, FALSE, FALSE, 24,
     8.50, 'USD', 29.99, 'USD', 950000, 2.5, 0.04, 3.0),

-- HOME_GOODS
('b1000000-0000-0000-0000-000000000014', 'HG-TWL-001', 'Egyptian Cotton Towel Set 6pc', 'HOME_GOODS',
    4.50, 16.0, 12.0,  6.0,FALSE, FALSE, FALSE, TRUE,  30,
    12.00, 'USD', 44.99, 'USD', 1600000, 3.2, 0.07, 3.5),

('b1000000-0000-0000-0000-000000000015', 'HG-CA-001', 'Stainless Steel French Press 34oz','HOME_GOODS',
    2.10,  9.0,  5.0, 13.0,FALSE, TRUE,  FALSE, FALSE, 48,
    10.00, 'USD', 34.99, 'USD', 1100000, 2.8, 0.08, 4.0),

('b1000000-0000-0000-0000-000000000016', 'HG-AIR-001', 'HEPA Air Purifier 500sqft',    'HOME_GOODS',
   11.00, 15.0, 10.0, 22.0,FALSE, FALSE, FALSE, FALSE,  6,
    55.00, 'USD', 159.99, 'USD', 680000, 1.9, 0.14, 6.0),

-- SPORTING_GOODS
('b1000000-0000-0000-0000-000000000017', 'SG-YG-001', 'Premium Yoga Mat 6mm Non-Slip',  'SPORTING_GOODS',
    2.80, 26.0,  8.0,  3.5,FALSE, FALSE, FALSE, TRUE,  30,
     9.00, 'USD', 32.99, 'USD', 2100000, 4.0, 0.11, 2.5),

('b1000000-0000-0000-0000-000000000018', 'SG-RB-001', 'Adjustable Resistance Bands Set','SPORTING_GOODS',
    1.50,  9.0,  6.0,  4.0,FALSE, FALSE, FALSE, TRUE,  120,
     5.50, 'USD', 22.99, 'USD', 3200000, 5.5, 0.16, 1.5),

-- APPAREL
('b1000000-0000-0000-0000-000000000019', 'AP-TS-001', 'Organic Cotton T-Shirt Unisex M','APPAREL',
    0.55,  9.0,  7.0,  1.5,FALSE, FALSE, FALSE, TRUE,  240,
     4.50, 'USD', 22.99, 'USD', 8000000, 7.0, 0.06, 2.0),

('b1000000-0000-0000-0000-000000000020', 'AP-JK-001', 'Lightweight Packable Rain Jacket','APPAREL',
    0.85, 10.0,  8.0,  3.0,FALSE, FALSE, FALSE, TRUE,  120,
    14.00, 'USD', 59.99, 'USD', 3500000, 3.8, 0.09, 3.0),

-- AUTOMOTIVE
('b1000000-0000-0000-0000-000000000021', 'AU-FIL-001', 'Premium Oil Filter 6-Pack',     'AUTOMOTIVE',
    2.40, 11.0,  7.0,  5.0,FALSE, FALSE, FALSE, TRUE,  72,
     9.50, 'USD', 24.99, 'USD', 4500000, 6.5, 0.04, 2.5),

('b1000000-0000-0000-0000-000000000022', 'AU-CAM-001', 'Backup Camera with Night Vision','AUTOMOTIVE',
    0.90,  8.0,  5.0,  3.0,FALSE, TRUE,  FALSE, TRUE,  96,
    18.00, 'USD', 54.99, 'USD', 1900000, 2.8, 0.12, 4.5);


-- ─── Supplier–Product links ───────────────────────────────────────────────────

INSERT INTO supplier_product (supplier_id, product_id, is_primary) VALUES
-- Pacific Rim Imports supplies electronics and apparel
('a1000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000001', TRUE),
('a1000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000002', TRUE),
('a1000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000003', TRUE),
('a1000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000004', TRUE),
('a1000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000019', FALSE),
('a1000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000020', TRUE),
-- Great Plains Wholesale supplies hardware/office
('a1000000-0000-0000-0000-000000000002','b1000000-0000-0000-0000-000000000008', TRUE),
('a1000000-0000-0000-0000-000000000002','b1000000-0000-0000-0000-000000000009', TRUE),
('a1000000-0000-0000-0000-000000000002','b1000000-0000-0000-0000-000000000010', TRUE),
('a1000000-0000-0000-0000-000000000002','b1000000-0000-0000-0000-000000000011', TRUE),
('a1000000-0000-0000-0000-000000000002','b1000000-0000-0000-0000-000000000012', TRUE),
('a1000000-0000-0000-0000-000000000002','b1000000-0000-0000-0000-000000000013', TRUE),
-- Eastern Manufacturing Co supplies health/beauty
('a1000000-0000-0000-0000-000000000003','b1000000-0000-0000-0000-000000000005', TRUE),
('a1000000-0000-0000-0000-000000000003','b1000000-0000-0000-0000-000000000006', TRUE),
('a1000000-0000-0000-0000-000000000003','b1000000-0000-0000-0000-000000000007', TRUE),
-- Southern Distribution supplies home goods + sporting
('a1000000-0000-0000-0000-000000000004','b1000000-0000-0000-0000-000000000014', TRUE),
('a1000000-0000-0000-0000-000000000004','b1000000-0000-0000-0000-000000000015', TRUE),
('a1000000-0000-0000-0000-000000000004','b1000000-0000-0000-0000-000000000017', TRUE),
('a1000000-0000-0000-0000-000000000004','b1000000-0000-0000-0000-000000000018', TRUE),
-- Midwest Direct supplies automotive + some hardware
('a1000000-0000-0000-0000-000000000005','b1000000-0000-0000-0000-000000000021', TRUE),
('a1000000-0000-0000-0000-000000000005','b1000000-0000-0000-0000-000000000022', TRUE),
('a1000000-0000-0000-0000-000000000005','b1000000-0000-0000-0000-000000000008', FALSE),
-- Pacific Coast Logistics (alt supplier for electronics + apparel)
('a1000000-0000-0000-0000-000000000006','b1000000-0000-0000-0000-000000000016', TRUE),
('a1000000-0000-0000-0000-000000000006','b1000000-0000-0000-0000-000000000019', TRUE),
('a1000000-0000-0000-0000-000000000006','b1000000-0000-0000-0000-000000000001', FALSE);


-- ─── Warehouses (≥4, mixed types) ────────────────────────────────────────────

INSERT INTO warehouse (id, name, type, state, zip, lat, lng,
                       ceiling_height_ft, pallet_capacity,
                       pick_fee_amount, pick_fee_currency,
                       receiving_fee_amount, receiving_fee_currency,
                       storage_fee_per_pallet_amount, storage_fee_per_pallet_currency)
VALUES
('c1000000-0000-0000-0000-000000000001',
    'Chicago Distribution Center', 'DC', 'IL', '60601', 41.8827, -87.6233,
    32.0, 8000,  1.25, 'USD', 18.00, 'USD', 22.00, 'USD'),

('c1000000-0000-0000-0000-000000000002',
    'Atlanta Fulfillment Center',  'FC', 'GA', '30301', 33.7490, -84.3880,
    28.0, 5000,  2.00, 'USD', 22.00, 'USD', 28.00, 'USD'),

('c1000000-0000-0000-0000-000000000003',
    'Los Angeles Cross Dock',      'CROSS_DOCK', 'CA', '90001', 34.0522, -118.2437,
    24.0, 2000,  0.75, 'USD', 12.00, 'USD',  0.00, 'USD'),

('c1000000-0000-0000-0000-000000000004',
    'Dallas 3PL Hub',              'PL3', 'TX', '75201', 32.7767, -96.7970,
    30.0, 6500,  1.50, 'USD', 16.00, 'USD', 20.00, 'USD'),

('c1000000-0000-0000-0000-000000000005',
    'Seattle Regional DC',         'DC',  'WA', '98101', 47.6062, -122.3321,
    36.0, 4000,  1.35, 'USD', 19.00, 'USD', 24.00, 'USD');


-- ─── Carriers (≥6: 3 parcel, 2 LTL, 1 FTL) ──────────────────────────────────

INSERT INTO carrier (id, name, scac, pricing_model,
                     liftgate_surcharge_amount, liftgate_surcharge_currency,
                     residential_surcharge_amount, residential_surcharge_currency,
                     dim_factor, fuel_surcharge_pct)
VALUES
-- Parcel carriers
('d1000000-0000-0000-0000-000000000001',
    'FastShip Parcel',      'FSPC', 'PARCEL',  0.00, 'USD',  4.90, 'USD', 139.0, 0.1650),

('d1000000-0000-0000-0000-000000000002',
    'SwiftBox Express',     'SWBX', 'PARCEL',  0.00, 'USD',  5.40, 'USD', 139.0, 0.1725),

('d1000000-0000-0000-0000-000000000003',
    'NationWide Ground',    'NWGD', 'PARCEL',  0.00, 'USD',  4.25, 'USD', 166.0, 0.1500),

-- LTL carriers
('d1000000-0000-0000-0000-000000000004',
    'Apex LTL Freight',     'AXLT', 'LTL',    85.00, 'USD', 75.00, 'USD',   NULL, 0.2200),

('d1000000-0000-0000-0000-000000000005',
    'Central States Freight','CSFR', 'LTL',   90.00, 'USD', 80.00, 'USD',   NULL, 0.2100),

-- FTL carrier
('d1000000-0000-0000-0000-000000000006',
    'TransCon Full Truckload','TCFT', 'FTL',  150.00, 'USD',  0.00, 'USD',   NULL, 0.2500),

-- Additional parcel (economy)
('d1000000-0000-0000-0000-000000000007',
    'EcoShip Budget Parcel', 'ECBP', 'PARCEL', 0.00, 'USD',  3.50, 'USD', 139.0, 0.1300);


-- ─── Carrier Lanes ────────────────────────────────────────────────────────────
-- Zone-based parcel rates (zones 1-8 from origin; transit days and per-lb rates vary)
-- FastShip Parcel — GROUND
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency, per_lb_rate,
                          min_charge_amount, min_charge_currency)
SELECT 'd1000000-0000-0000-0000-000000000001', 'GROUND', oz::TEXT, dz::TEXT,
       CASE WHEN ABS(oz - dz) <= 1 THEN 1
            WHEN ABS(oz - dz) <= 3 THEN 2
            WHEN ABS(oz - dz) <= 5 THEN 4
            ELSE 6 END,
       7.25 + (ABS(oz - dz) * 0.40), 'USD',
       0.1450 + (ABS(oz - dz) * 0.0080),
       8.50, 'USD'
FROM generate_series(1,8) oz, generate_series(1,8) dz;

-- FastShip Parcel — EXPRESS (2-day)
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency, per_lb_rate,
                          min_charge_amount, min_charge_currency)
SELECT 'd1000000-0000-0000-0000-000000000001', 'EXPRESS', oz::TEXT, dz::TEXT,
       2, 18.00 + (ABS(oz - dz) * 0.60), 'USD',
       0.2800 + (ABS(oz - dz) * 0.0120),
       20.00, 'USD'
FROM generate_series(1,8) oz, generate_series(1,8) dz;

-- SwiftBox Express — OVERNIGHT
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency, per_lb_rate,
                          min_charge_amount, min_charge_currency)
SELECT 'd1000000-0000-0000-0000-000000000002', 'OVERNIGHT', oz::TEXT, dz::TEXT,
       1, 32.00 + (ABS(oz - dz) * 0.80), 'USD',
       0.4500 + (ABS(oz - dz) * 0.0180),
       35.00, 'USD'
FROM generate_series(1,8) oz, generate_series(1,8) dz;

-- SwiftBox Express — GROUND
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency, per_lb_rate,
                          min_charge_amount, min_charge_currency)
SELECT 'd1000000-0000-0000-0000-000000000002', 'GROUND', oz::TEXT, dz::TEXT,
       CASE WHEN ABS(oz - dz) <= 1 THEN 1
            WHEN ABS(oz - dz) <= 3 THEN 2
            WHEN ABS(oz - dz) <= 5 THEN 4
            ELSE 6 END,
       7.50 + (ABS(oz - dz) * 0.42), 'USD',
       0.1500 + (ABS(oz - dz) * 0.0085),
       9.00, 'USD'
FROM generate_series(1,8) oz, generate_series(1,8) dz;

-- NationWide Ground — GROUND and ECONOMY
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency, per_lb_rate,
                          min_charge_amount, min_charge_currency)
SELECT 'd1000000-0000-0000-0000-000000000003', 'GROUND', oz::TEXT, dz::TEXT,
       CASE WHEN ABS(oz - dz) <= 1 THEN 2
            WHEN ABS(oz - dz) <= 3 THEN 3
            WHEN ABS(oz - dz) <= 5 THEN 5
            ELSE 7 END,
       6.80 + (ABS(oz - dz) * 0.38), 'USD',
       0.1380 + (ABS(oz - dz) * 0.0075),
       7.50, 'USD'
FROM generate_series(1,8) oz, generate_series(1,8) dz;

-- EcoShip Budget — ECONOMY
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency, per_lb_rate,
                          min_charge_amount, min_charge_currency)
SELECT 'd1000000-0000-0000-0000-000000000007', 'ECONOMY', oz::TEXT, dz::TEXT,
       CASE WHEN ABS(oz - dz) <= 2 THEN 3
            WHEN ABS(oz - dz) <= 4 THEN 5
            ELSE 8 END,
       5.50 + (ABS(oz - dz) * 0.32), 'USD',
       0.1100 + (ABS(oz - dz) * 0.0060),
       6.00, 'USD'
FROM generate_series(1,8) oz, generate_series(1,8) dz;

-- Apex LTL — LTL lanes (state-pair-level, simplified as origin/dest zone proxies)
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency,
                          per_cwt_rate, min_charge_amount, min_charge_currency)
VALUES
('d1000000-0000-0000-0000-000000000004', 'LTL', '1', '1', 1, 150.00, 'USD', 18.50, 250.00, 'USD'),
('d1000000-0000-0000-0000-000000000004', 'LTL', '1', '2', 2, 150.00, 'USD', 20.00, 250.00, 'USD'),
('d1000000-0000-0000-0000-000000000004', 'LTL', '1', '3', 3, 150.00, 'USD', 22.00, 250.00, 'USD'),
('d1000000-0000-0000-0000-000000000004', 'LTL', '2', '1', 2, 150.00, 'USD', 20.00, 250.00, 'USD'),
('d1000000-0000-0000-0000-000000000004', 'LTL', '2', '2', 1, 150.00, 'USD', 18.50, 250.00, 'USD'),
('d1000000-0000-0000-0000-000000000004', 'LTL', '2', '3', 2, 150.00, 'USD', 20.50, 250.00, 'USD'),
('d1000000-0000-0000-0000-000000000004', 'LTL', '3', '1', 3, 150.00, 'USD', 22.00, 250.00, 'USD'),
('d1000000-0000-0000-0000-000000000004', 'LTL', '3', '2', 2, 150.00, 'USD', 20.50, 250.00, 'USD'),
('d1000000-0000-0000-0000-000000000004', 'LTL', '3', '3', 1, 150.00, 'USD', 19.00, 250.00, 'USD');

-- Central States Freight — LTL
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency,
                          per_cwt_rate, min_charge_amount, min_charge_currency)
VALUES
('d1000000-0000-0000-0000-000000000005', 'LTL', '1', '1', 1, 140.00, 'USD', 17.50, 240.00, 'USD'),
('d1000000-0000-0000-0000-000000000005', 'LTL', '1', '2', 2, 140.00, 'USD', 19.00, 240.00, 'USD'),
('d1000000-0000-0000-0000-000000000005', 'LTL', '1', '3', 3, 140.00, 'USD', 21.00, 240.00, 'USD'),
('d1000000-0000-0000-0000-000000000005', 'LTL', '2', '1', 2, 140.00, 'USD', 19.00, 240.00, 'USD'),
('d1000000-0000-0000-0000-000000000005', 'LTL', '2', '2', 1, 140.00, 'USD', 17.50, 240.00, 'USD'),
('d1000000-0000-0000-0000-000000000005', 'LTL', '2', '3', 2, 140.00, 'USD', 19.50, 240.00, 'USD'),
('d1000000-0000-0000-0000-000000000005', 'LTL', '3', '1', 3, 140.00, 'USD', 21.00, 240.00, 'USD'),
('d1000000-0000-0000-0000-000000000005', 'LTL', '3', '2', 2, 140.00, 'USD', 19.50, 240.00, 'USD'),
('d1000000-0000-0000-0000-000000000005', 'LTL', '3', '3', 1, 140.00, 'USD', 18.00, 240.00, 'USD');

-- TransCon FTL — flat per-mile pricing proxied via zone pairs
INSERT INTO carrier_lane (carrier_id, service_level, origin_zone, dest_zone,
                          transit_days, base_rate_amount, base_rate_currency,
                          min_charge_amount, min_charge_currency)
VALUES
('d1000000-0000-0000-0000-000000000006', 'FTL', '1', '1',  1,  1200.00, 'USD', 1200.00, 'USD'),
('d1000000-0000-0000-0000-000000000006', 'FTL', '1', '2',  2,  1800.00, 'USD', 1800.00, 'USD'),
('d1000000-0000-0000-0000-000000000006', 'FTL', '1', '3',  3,  2600.00, 'USD', 2600.00, 'USD'),
('d1000000-0000-0000-0000-000000000006', 'FTL', '2', '1',  2,  1800.00, 'USD', 1800.00, 'USD'),
('d1000000-0000-0000-0000-000000000006', 'FTL', '2', '2',  1,  1250.00, 'USD', 1250.00, 'USD'),
('d1000000-0000-0000-0000-000000000006', 'FTL', '2', '3',  2,  1950.00, 'USD', 1950.00, 'USD'),
('d1000000-0000-0000-0000-000000000006', 'FTL', '3', '1',  3,  2600.00, 'USD', 2600.00, 'USD'),
('d1000000-0000-0000-0000-000000000006', 'FTL', '3', '2',  2,  1950.00, 'USD', 1950.00, 'USD'),
('d1000000-0000-0000-0000-000000000006', 'FTL', '3', '3',  1,  1300.00, 'USD', 1300.00, 'USD');


-- ─── Competitors (≥6) ────────────────────────────────────────────────────────

INSERT INTO competitor (id, name, estimated_margin, distribution_model,
                        num_warehouses, avg_transit_days, delivery_speed, regional_presence)
VALUES
('e1000000-0000-0000-0000-000000000001',
    'Apex Distribution Corp',   0.2200, 'DIRECT',       12, 2, 'TWO_DAY',
    ARRAY['NORTHEAST','MIDWEST','SOUTHEAST']),

('e1000000-0000-0000-0000-000000000002',
    'GlobalSource Wholesale',   0.1800, 'DISTRIBUTOR',   8, 4, 'STANDARD',
    ARRAY['WEST','SOUTHWEST']),

('e1000000-0000-0000-0000-000000000003',
    'FastTrack Supply Co',      0.2500, 'DIRECT',       20, 1, 'NEXT_DAY',
    ARRAY['NORTHEAST','SOUTHEAST','MIDWEST','SOUTHWEST','WEST']),

('e1000000-0000-0000-0000-000000000004',
    'ValueMax Distributors',    0.1200, 'DISTRIBUTOR',   4, 5, 'STANDARD',
    ARRAY['MIDWEST','SOUTHWEST']),

('e1000000-0000-0000-0000-000000000005',
    'PrimeReach Logistics',     0.2800, 'HYBRID',       15, 2, 'TWO_DAY',
    ARRAY['NORTHEAST','SOUTHEAST','WEST']),

('e1000000-0000-0000-0000-000000000006',
    'Marketplace Direct Inc',   0.1500, 'MARKETPLACE',   0, 3, 'TWO_DAY',
    ARRAY['NORTHEAST','SOUTHEAST','MIDWEST','SOUTHWEST','WEST']),

('e1000000-0000-0000-0000-000000000007',
    'RegionFirst Supply',       0.2000, 'DIRECT',        3, 2, 'TWO_DAY',
    ARRAY['SOUTHEAST']),

('e1000000-0000-0000-0000-000000000008',
    'Central Bulk Wholesale',   0.1600, 'DISTRIBUTOR',   6, 4, 'STANDARD',
    ARRAY['MIDWEST','SOUTHWEST','SOUTHEAST']);
