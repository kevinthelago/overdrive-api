# overdrive-api

![License](https://img.shields.io/github/license/kevinthelago/overdrive-api) ![Last commit](https://img.shields.io/github/last-commit/kevinthelago/overdrive-api)

# Distribution Opportunity Engine

## Overview

# Distribution Opportunity Engine

## Tech stack

# Stack

## Backend (API & engines)

| Layer | Choice | Notes |
|---|---|---|
| Language | **Kotlin 2.x** on **JDK 21** | DDD-friendly, null-safe domain model |
| Framework | **Spring Boot 3.x** | Web, Data JPA, Validation, Actuator |
| Build | **Gradle (Kotlin DSL)** | `./gradlew` |
| Datastore | **PostgreSQL 16** | Spring Data JPA + **Flyway** migrations |
| Cache | **Redis 7** | Caches deterministic engine outputs (delivered-cost / routing / opportunity results keyed by inputs) and scenario computations. Safe because calculations are reproducible. |
| Events | **In-process Spring `ApplicationEventPublisher`** | Domain events for recompute / cache-invalidation / audit-trace. **No external broker** — single-user, deterministic; a queue is unnecessary. Kafka/RabbitMQ deferred to future work. |
| Testing | **JUnit 5 + Kotest + MockK + Testcontainers** | Testcontainers spins real Postgres/Redis for integration tests |

## Frontend (analyst UI)

| Layer | Choice | Notes |
|---|---|---|
| Framework | **React 18 + TypeScript 5** | |
| Build | **Vite** | |
| Package manager | **pnpm** | |
| Server state | **TanStack Query** | caches API reads, invalidation on mutations |
| Routing | **React Router** | |
| Styling | **Tailwind CSS** + headless components (Radix primitives) | |
| Visualization | **D3** (`d3`, `d3-geo`, `topojson-client`, `us-atlas`) | Low-level foundation chosen over a preset charting lib so charts (cost-breakdown sankey/waterfall, freight flows, US geographic heat maps, opportunity rankings) can be extended thoroughly later without library limits. |
| Testing | **Vitest + React Testing Library**, **Playwright** (E2E) | |

## Cross-cutting principles (from the pitch)

- **Domain-driven design** — rich Kotlin aggregates, value objects for money/dimensions/cost components.
- **Event-driven** — in-process domain events only (v1).
- **Deterministic calculations** — same inputs → same outputs; everything cacheable and reproducible.
- **Explainability over magic** — every engine emits a traceable decision record.

## Local dev

- **Docker Compose** brings up Postgres + Redis for local development; the backend and frontend run on the host.

## Getting started

```bash
git clone https://github.com/kevinthelago/overdrive-api.git
cd overdrive-api
# install dependencies and run the project's build/test/dev commands
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md).

## License

See [LICENSE](LICENSE).

---

_Scaffolded by base-studio-code._