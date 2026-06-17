-- V6: opportunity_projection table
-- Stores the pre-computed opportunity score per product (and per scenario override).
-- Written by OpportunityEngineService on every recompute cycle; rows are replaced
-- (delete+insert) rather than updated to keep the schema simple.
-- A NULL scenario_id row is the baseline; non-null rows are scenario snapshots.

CREATE TABLE opportunity_projection
(
    id                    UUID           NOT NULL DEFAULT gen_random_uuid(),
    product_id            UUID           NOT NULL,
    category              VARCHAR(100)   NOT NULL,
    score                 NUMERIC(12, 6) NOT NULL,
    savings_pct           NUMERIC(10, 6),
    market_size_usd       NUMERIC(20, 2),
    order_frequency       NUMERIC(10, 6),
    category_growth_pct   NUMERIC(10, 6),
    supplier_availability NUMERIC(10, 6),
    logistics_complexity  NUMERIC(10, 6),
    -- JSONB array of { region, representativeZip, score, savingsPct }
    region_breakdown_json JSONB,
    zero_reason           VARCHAR(64),
    scenario_id           UUID,
    computed_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT opportunity_projection_pkey PRIMARY KEY (id),

    CONSTRAINT fk_op_product FOREIGN KEY (product_id)
        REFERENCES product (id) ON DELETE CASCADE,

    -- Unique baseline per product; unique per product+scenario.
    CONSTRAINT uq_op_product_scenario
        UNIQUE NULLS NOT DISTINCT (product_id, scenario_id)
);

CREATE INDEX idx_op_score_baseline
    ON opportunity_projection (score DESC)
    WHERE scenario_id IS NULL;

CREATE INDEX idx_op_score_scenario
    ON opportunity_projection (scenario_id, score DESC)
    WHERE scenario_id IS NOT NULL;

CREATE INDEX idx_op_product_scenario
    ON opportunity_projection (product_id, scenario_id);

CREATE INDEX idx_op_category
    ON opportunity_projection (category, scenario_id);
