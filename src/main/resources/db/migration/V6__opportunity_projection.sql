-- V6: opportunity_projection table
-- Stores the pre-computed opportunity score per product (and per scenario override).
-- Written by OpportunityEngineService on every recompute cycle; rows are replaced
-- (delete+insert) rather than updated to keep audit history trivial.
-- A NULL scenario_id row is the baseline; non-null rows are scenario snapshots.

CREATE TABLE opportunity_projection
(
    id                    UUID        NOT NULL DEFAULT gen_random_uuid(),
    product_id            BIGINT      NOT NULL,
    category_id           BIGINT      NOT NULL,
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
    computed_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT opportunity_projection_pkey PRIMARY KEY (id),

    -- Enforce FK integrity once catalog products table exists (added by catalog stream, V2).
    CONSTRAINT fk_op_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE,

    -- Unique baseline per product; unique per product+scenario.
    CONSTRAINT uq_op_product_baseline
        UNIQUE NULLS NOT DISTINCT (product_id, scenario_id)
);

-- Fast ranked reads by score (baseline query).
CREATE INDEX idx_op_score_baseline
    ON opportunity_projection (score DESC)
    WHERE scenario_id IS NULL;

-- Fast ranked reads by score per scenario.
CREATE INDEX idx_op_score_scenario
    ON opportunity_projection (scenario_id, score DESC)
    WHERE scenario_id IS NOT NULL;

-- Fast lookup for recompute upserts.
CREATE INDEX idx_op_product_scenario
    ON opportunity_projection (product_id, scenario_id);

-- Category aggregation queries.
CREATE INDEX idx_op_category
    ON opportunity_projection (category_id, scenario_id);
