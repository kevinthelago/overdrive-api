-- V8: Analytics read-model — denormalized projection tables
-- Owned by stream scenario-analytics-api
--
-- These tables are populated by event-listener projectors in the analytics module
-- (CostComputedProjector, RouteProjector, OpportunityProjector, CompetitorProjector).
-- They are NOT views: denormalization keeps scenario-aware GROUP BY queries O(1) per row.
-- NULL scenario_id means baseline.

-- Route-level cost breakdown per product × destination × scenario
CREATE TABLE analytics_route_summary (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id            UUID         NOT NULL,
    scenario_id           UUID,
    destination_zip       VARCHAR(10),
    destination_region    VARCHAR(20),
    warehouse_id          UUID,
    carrier_id            UUID,
    fulfillment_model     VARCHAR(50),
    service_level         VARCHAR(50),
    transit_days          INTEGER,
    product_cost_usd      NUMERIC(19,4),
    storage_cost_usd      NUMERIC(19,4),
    handling_cost_usd     NUMERIC(19,4),
    freight_cost_usd      NUMERIC(19,4),
    fees_cost_usd         NUMERIC(19,4),
    reserve_cost_usd      NUMERIC(19,4),
    other_cost_usd        NUMERIC(19,4),
    total_delivered_cost_usd NUMERIC(19,4),
    computed_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ars_product     ON analytics_route_summary(product_id);
CREATE INDEX idx_ars_scenario    ON analytics_route_summary(scenario_id);
CREATE INDEX idx_ars_warehouse   ON analytics_route_summary(warehouse_id);
CREATE INDEX idx_ars_carrier     ON analytics_route_summary(carrier_id);
CREATE INDEX idx_ars_region      ON analytics_route_summary(destination_region);

-- Competitor savings per product × competitor × scenario
CREATE TABLE analytics_competitor_summary (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id              UUID         NOT NULL,
    competitor_id           UUID         NOT NULL,
    scenario_id             UUID,
    our_delivered_cost_usd  NUMERIC(19,4),
    competitor_price_usd    NUMERIC(19,4),
    savings_pct             NUMERIC(9,4),
    is_offered              BOOLEAN      NOT NULL DEFAULT TRUE,
    not_offered_reason      VARCHAR(255),
    computed_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_acs_product   ON analytics_competitor_summary(product_id);
CREATE INDEX idx_acs_scenario  ON analytics_competitor_summary(scenario_id);
CREATE INDEX idx_acs_competitor ON analytics_competitor_summary(competitor_id);

-- Opportunity scores per product × region × scenario (drives choropleth + rankings)
CREATE TABLE analytics_opportunity_summary (
    id                         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id                 UUID         NOT NULL,
    category                   VARCHAR(100),
    scenario_id                UUID,
    region                     VARCHAR(20),
    opportunity_score          NUMERIC(12,6),
    savings_pct_factor         NUMERIC(9,4),
    market_size_factor         NUMERIC(9,4),
    order_frequency_factor     NUMERIC(9,4),
    category_growth_factor     NUMERIC(9,4),
    supplier_availability_factor NUMERIC(9,4),
    logistics_complexity_factor  NUMERIC(9,4),
    no_route_reason            VARCHAR(255),
    computed_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_aos_product   ON analytics_opportunity_summary(product_id);
CREATE INDEX idx_aos_scenario  ON analytics_opportunity_summary(scenario_id);
CREATE INDEX idx_aos_region    ON analytics_opportunity_summary(region);
CREATE INDEX idx_aos_category  ON analytics_opportunity_summary(category);
CREATE INDEX idx_aos_score     ON analytics_opportunity_summary(opportunity_score DESC);

-- Warehouse utilization per scenario (capacity vs assigned)
CREATE TABLE analytics_warehouse_utilization (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id     UUID         NOT NULL,
    scenario_id      UUID,
    pallet_capacity  INTEGER,
    assigned_pallets INTEGER,
    utilization_pct  NUMERIC(9,4),
    computed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_awu_warehouse ON analytics_warehouse_utilization(warehouse_id);
CREATE INDEX idx_awu_scenario  ON analytics_warehouse_utilization(scenario_id);
