CREATE TABLE engine_traces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    engine_type VARCHAR(50) NOT NULL,
    opportunity_id UUID NOT NULL,
    scenario_context_id UUID,
    inputs JSONB NOT NULL,
    outputs JSONB NOT NULL,
    steps JSONB NOT NULL DEFAULT '[]'::jsonb,
    computed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_engine_traces_opportunity ON engine_traces (opportunity_id);
CREATE INDEX idx_engine_traces_engine_type ON engine_traces (engine_type);
CREATE INDEX idx_engine_traces_computed_at ON engine_traces (computed_at DESC);
