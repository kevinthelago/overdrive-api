-- V7: Scenario aggregate — ordered typed overrides over the baseline catalog
-- Owned by stream scenario-analytics-api

CREATE TABLE scenario (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE scenario_override (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id   UUID         NOT NULL REFERENCES scenario(id) ON DELETE CASCADE,
    override_type VARCHAR(50)  NOT NULL,
    -- ordered application: lower position applied first; no two overrides share the same position
    position      INTEGER      NOT NULL,
    -- optional reference to the catalog entity being acted on
    entity_id     UUID,
    -- type-specific parameters stored as JSON (e.g. deltaPct for SUPPLIER_PRICE_DELTA)
    params        JSONB        NOT NULL DEFAULT '{}',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT scenario_override_position_unique UNIQUE (scenario_id, position),
    CONSTRAINT scenario_override_type_check CHECK (
        override_type IN (
            'ADD_WAREHOUSE',
            'REMOVE_WAREHOUSE',
            'ADD_CARRIER',
            'REMOVE_CARRIER',
            'SUPPLIER_PRICE_DELTA',
            'FEE_SCHEDULE_CHANGE',
            'DEMAND_FACTOR_CHANGE'
        )
    )
);

CREATE INDEX idx_scenario_override_scenario_id ON scenario_override(scenario_id);
