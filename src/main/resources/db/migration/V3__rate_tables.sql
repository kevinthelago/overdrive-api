-- V3: Rate tables — carrier lanes, category payment/return rates, ZIP centroids

-- Carrier lane: origin-zone → dest-zone rate card per service level.
-- Zones are typically 1-8 for parcel carriers; LTL/FTL use state pairs or custom regions.
-- Prices stored as Money pairs; per-unit rates as plain NUMERIC.
CREATE TABLE carrier_lane (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    carrier_id              UUID            NOT NULL REFERENCES carrier(id),

    -- Service level offered on this lane (GROUND | EXPRESS | OVERNIGHT | LTL | FTL | ECONOMY)
    service_level           VARCHAR(20)     NOT NULL,

    -- Zone-based routing (parcel model: origin zone 1-8, dest zone 1-8)
    origin_zone             VARCHAR(10),
    dest_zone               VARCHAR(10),

    -- ZIP-prefix routing (first 3 digits for broader matching; exact ZIP for spot rates)
    origin_zip_prefix       VARCHAR(5),
    dest_zip_prefix         VARCHAR(5),

    -- Estimated transit time for this lane + service level
    transit_days            INTEGER         NOT NULL CHECK (transit_days >= 0),

    -- Base rate (Money) — flat charge before weight/cube tiers
    base_rate_amount        NUMERIC(18,4)   NOT NULL CHECK (base_rate_amount >= 0),
    base_rate_currency      VARCHAR(3)      NOT NULL DEFAULT 'USD',

    -- Incremental per-lb rate (parcel / LTL billable weight)
    per_lb_rate             NUMERIC(10,6)   CHECK (per_lb_rate >= 0),

    -- Per-cwt (hundredweight) rate (LTL class pricing)
    per_cwt_rate            NUMERIC(10,4)   CHECK (per_cwt_rate >= 0),

    -- Minimum charge — floor applied when base+weight rate falls below this
    min_charge_amount       NUMERIC(18,4)   CHECK (min_charge_amount >= 0),
    min_charge_currency     VARCHAR(3)      NOT NULL DEFAULT 'USD',

    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_carrier_lane UNIQUE (carrier_id, service_level, origin_zone, dest_zone)
);

CREATE INDEX ix_carrier_lane_carrier     ON carrier_lane (carrier_id);
CREATE INDEX ix_carrier_lane_service     ON carrier_lane (service_level);
CREATE INDEX ix_carrier_lane_zones       ON carrier_lane (origin_zone, dest_zone);
CREATE INDEX ix_carrier_lane_zip_prefix  ON carrier_lane (origin_zip_prefix, dest_zip_prefix);


-- Category-level payment and return rate assumptions used by the cost / opportunity engines.
-- payment_rate: typical net-payment-terms discount rate for invoices in this category.
-- return_rate: expected fraction of units returned (reverse logistics cost driver).
CREATE TABLE category_rate (
    category            VARCHAR(100)    PRIMARY KEY,
    payment_rate        NUMERIC(5,4)    NOT NULL CHECK (payment_rate BETWEEN 0 AND 1),
    return_rate         NUMERIC(5,4)    NOT NULL CHECK (return_rate BETWEEN 0 AND 1),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


-- Static ZIP-centroid lookup used by routing and transit-estimation engines.
-- Populated once; never mutated by application code.
CREATE TABLE zip_centroid (
    zip         VARCHAR(10)         PRIMARY KEY,
    lat         DOUBLE PRECISION    NOT NULL,
    lng         DOUBLE PRECISION    NOT NULL,
    city        VARCHAR(100),
    state       VARCHAR(2)          NOT NULL,
    -- Geographic macro-region for zone lookups and competitor-presence matching
    region      VARCHAR(20)         NOT NULL    -- NORTHEAST | SOUTHEAST | MIDWEST | SOUTHWEST | WEST
);

CREATE INDEX ix_zip_centroid_state  ON zip_centroid (state);
CREATE INDEX ix_zip_centroid_region ON zip_centroid (region);
