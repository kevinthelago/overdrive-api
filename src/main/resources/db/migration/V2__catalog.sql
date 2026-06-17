-- V2: Catalog aggregates — Product, Supplier, Warehouse, Carrier, Competitor
-- All monetary values: NUMERIC(18,4) + VARCHAR(3) currency pair (mirrors Money primitive)
-- Geo coordinates: DOUBLE PRECISION lat/lng (mirrors Geo primitive)
-- All aggregates carry a @Version column for JPA optimistic locking

CREATE TABLE product (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    version                 BIGINT          NOT NULL DEFAULT 0,

    sku                     VARCHAR(100)    NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    category                VARCHAR(100)    NOT NULL,

    -- Weight (mirrors Weight value object: value + unit)
    weight_value            NUMERIC(10,3)   NOT NULL CHECK (weight_value > 0),
    weight_unit             VARCHAR(10)     NOT NULL DEFAULT 'LB',

    -- Dimensions in inches (mirrors Dimensions value object: length/width/height)
    length_in               NUMERIC(10,2)   NOT NULL CHECK (length_in > 0),
    width_in                NUMERIC(10,2)   NOT NULL CHECK (width_in > 0),
    height_in               NUMERIC(10,2)   NOT NULL CHECK (height_in > 0),

    -- Handling flags
    hazardous               BOOLEAN         NOT NULL DEFAULT FALSE,
    fragile                 BOOLEAN         NOT NULL DEFAULT FALSE,
    temperature_sensitive   BOOLEAN         NOT NULL DEFAULT FALSE,
    stackable               BOOLEAN         NOT NULL DEFAULT TRUE,

    pallet_qty              INTEGER         NOT NULL DEFAULT 1 CHECK (pallet_qty >= 1),

    -- Cost (Money)
    cost_amount             NUMERIC(18,4)   NOT NULL CHECK (cost_amount >= 0),
    cost_currency           VARCHAR(3)      NOT NULL DEFAULT 'USD',

    -- MSRP (Money)
    msrp_amount             NUMERIC(18,4)   NOT NULL CHECK (msrp_amount >= 0),
    msrp_currency           VARCHAR(3)      NOT NULL DEFAULT 'USD',

    -- Demand / market-intelligence fields
    market_size             NUMERIC(18,2),                          -- estimated addressable units/yr
    order_frequency         NUMERIC(10,4),                          -- avg orders per customer per year
    category_growth         NUMERIC(10,4),                          -- YoY growth rate (e.g. 0.12 = 12%)
    logistics_complexity    NUMERIC(5,2)    CHECK (logistics_complexity BETWEEN 0 AND 10),

    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_product_sku UNIQUE (sku)
);

CREATE INDEX ix_product_category ON product (category);


CREATE TABLE supplier (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    version                 BIGINT          NOT NULL DEFAULT 0,

    name                    VARCHAR(255)    NOT NULL,

    -- Minimum order quantity (units)
    moq                     INTEGER         NOT NULL DEFAULT 1 CHECK (moq >= 1),

    -- Lead time in calendar days
    lead_time_days          INTEGER         NOT NULL CHECK (lead_time_days >= 0),

    -- Per-unit acquisition cost (Money) — NULL when cost is negotiated per-order
    cost_amount             NUMERIC(18,4)   CHECK (cost_amount >= 0),
    cost_currency           VARCHAR(3)      NOT NULL DEFAULT 'USD',

    -- Shipping origin (Geo)
    origin_zip              VARCHAR(10),
    origin_lat              DOUBLE PRECISION,
    origin_lng              DOUBLE PRECISION,

    -- Volume discount as a decimal fraction (e.g. 0.05 = 5% off above MOQ threshold)
    volume_discount_pct     NUMERIC(5,4)    NOT NULL DEFAULT 0 CHECK (volume_discount_pct BETWEEN 0 AND 1),

    -- Historical reliability (0.0 = never on-time, 1.0 = always on-time)
    reliability_score       NUMERIC(5,4)    NOT NULL DEFAULT 1 CHECK (reliability_score BETWEEN 0 AND 1),

    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


-- Products may have multiple suppliers; this junction captures the relationship.
CREATE TABLE supplier_product (
    supplier_id             UUID            NOT NULL REFERENCES supplier(id),
    product_id              UUID            NOT NULL REFERENCES product(id),
    is_primary              BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (supplier_id, product_id)
);

CREATE INDEX ix_supplier_product_product ON supplier_product (product_id);


CREATE TABLE warehouse (
    id                              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    version                         BIGINT          NOT NULL DEFAULT 0,

    name                            VARCHAR(255)    NOT NULL,

    -- Type captures functional role: DC=distribution center, FC=fulfillment center,
    -- CROSS_DOCK=cross-docking, PL3=third-party logistics
    type                            VARCHAR(20)     NOT NULL,

    state                           VARCHAR(2)      NOT NULL,
    zip                             VARCHAR(10)     NOT NULL,
    lat                             DOUBLE PRECISION NOT NULL,
    lng                             DOUBLE PRECISION NOT NULL,

    ceiling_height_ft               NUMERIC(6,2),
    pallet_capacity                 INTEGER         NOT NULL CHECK (pallet_capacity > 0),

    -- Operating fees (Money per event)
    pick_fee_amount                 NUMERIC(18,4)   NOT NULL DEFAULT 0 CHECK (pick_fee_amount >= 0),
    pick_fee_currency               VARCHAR(3)      NOT NULL DEFAULT 'USD',

    receiving_fee_amount            NUMERIC(18,4)   NOT NULL DEFAULT 0 CHECK (receiving_fee_amount >= 0),
    receiving_fee_currency          VARCHAR(3)      NOT NULL DEFAULT 'USD',

    storage_fee_per_pallet_amount   NUMERIC(18,4)   NOT NULL DEFAULT 0 CHECK (storage_fee_per_pallet_amount >= 0),
    storage_fee_per_pallet_currency VARCHAR(3)      NOT NULL DEFAULT 'USD',

    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_warehouse_state ON warehouse (state);
CREATE INDEX ix_warehouse_zip   ON warehouse (zip);


CREATE TABLE carrier (
    id                              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    version                         BIGINT          NOT NULL DEFAULT 0,

    name                            VARCHAR(255)    NOT NULL,
    -- Standard Carrier Alpha Code (optional, used for EDI/track integrations)
    scac                            VARCHAR(10),

    -- Primary pricing model driving lane-table structure
    pricing_model                   VARCHAR(20)     NOT NULL,   -- PARCEL | LTL | FTL

    -- Accessorial surcharges (Money flat fee applied per shipment where applicable)
    liftgate_surcharge_amount       NUMERIC(18,4)   NOT NULL DEFAULT 0,
    liftgate_surcharge_currency     VARCHAR(3)      NOT NULL DEFAULT 'USD',

    residential_surcharge_amount    NUMERIC(18,4)   NOT NULL DEFAULT 0,
    residential_surcharge_currency  VARCHAR(3)      NOT NULL DEFAULT 'USD',

    -- Dimensional weight divisor (in³ per lb; e.g. 139 for domestic ground)
    dim_factor                      NUMERIC(10,4),

    -- Fuel surcharge as a decimal fraction applied on top of base rate
    fuel_surcharge_pct              NUMERIC(5,4)    NOT NULL DEFAULT 0 CHECK (fuel_surcharge_pct BETWEEN 0 AND 1),

    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_carrier_scac UNIQUE (scac)
);


CREATE TABLE competitor (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    version                 BIGINT          NOT NULL DEFAULT 0,

    name                    VARCHAR(255)    NOT NULL,

    -- Estimated gross margin (0.0–1.0)
    estimated_margin        NUMERIC(5,4)    CHECK (estimated_margin BETWEEN 0 AND 1),

    -- How they go to market
    distribution_model      VARCHAR(30),    -- DIRECT | DISTRIBUTOR | HYBRID | MARKETPLACE

    num_warehouses          INTEGER         CHECK (num_warehouses >= 0),

    -- Shipping assumptions used by the opportunity engine
    avg_transit_days        INTEGER         CHECK (avg_transit_days >= 0),
    delivery_speed          VARCHAR(20),    -- SAME_DAY | NEXT_DAY | TWO_DAY | STANDARD

    -- Regions where they compete actively (e.g. '{NORTHEAST,SOUTHEAST}')
    regional_presence       TEXT[]          NOT NULL DEFAULT '{}',

    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
