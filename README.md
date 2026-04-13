# Utility Billing Engine

A meter-to-cash billing engine built with Java 17, Spring Boot 3.2, and Oracle DB. Processes meter reads, calculates bills using configurable tariff rules, manages billing cycles, and handles payment processing — modeled after Oracle Utilities CC&B patterns.

## Architecture

```
                    ┌─────────────────────────────────────────────┐
                    │              REST API Layer                  │
                    │  /meters  /bills  /payments  /accounts      │
                    └──────┬──────────────┬───────────┬───────────┘
                           │              │           │
              ┌────────────▼──┐    ┌──────▼─────┐  ┌─▼──────────────┐
              │ MeterIngestion │    │  Billing   │  │    Payment     │
              │   Service      │    │  Engine    │  │    Service     │
              └────────┬───────┘    └──────┬─────┘  └──────┬─────────┘
                       │                   │               │
              ┌────────▼───────────────────▼───────────────▼─────────┐
              │              Groovy Scripting Engine                   │
              │   (Configurable rate rules, validation, plug-ins)     │
              └────────────────────────┬──────────────────────────────┘
                                       │
              ┌────────────────────────▼──────────────────────────────┐
              │                 Spring Batch Layer                     │
              │   BillingRunJob  │  PaymentPostJob  │  AgingJob       │
              └────────────────────────┬──────────────────────────────┘
                                       │
              ┌────────────────────────▼──────────────────────────────┐
              │                    Oracle DB                           │
              │  ACCOUNTS │ METER_READS │ BILLS │ PAYMENTS │ TARIFFS  │
              └───────────────────────────────────────────────────────┘
```

## Key Design Decisions

### Why Groovy Scripting?
Oracle Utilities CC&B uses configurable service scripts and plug-in spots for business logic. We replicate this pattern with a Groovy-based scripting engine that loads billing rules at runtime. Billing analysts can modify tariff calculations, validation rules, and payment allocation logic without redeploying the application.

### Why Spring Batch?
Utility billing is inherently batch-oriented — billing runs process all accounts in a cycle, payment files are posted in bulk, and account aging runs nightly. Spring Batch gives us chunk-oriented processing with built-in checkpoint restart, skip/retry policies, and job monitoring.

### Meter-to-Cash Flow
1. **Meter Read Ingestion** — Reads arrive via REST API or batch file upload
2. **Validation** — Groovy scripts validate reads (high/low checks, zero reads, estimated reads)
3. **Bill Calculation** — Tariff engine applies rate schedules based on service class, usage tier, and season
4. **Bill Review** — Bills in PENDING state can be reviewed before approval
5. **Payment Processing** — Payments applied to oldest bills first (FIFO), with partial payment support
6. **Aging** — Nightly batch ages unpaid balances (30/60/90-day buckets)

### Data Model
- **Account** — Customer account with service class and billing cycle
- **MeterRead** — Raw and validated meter readings with read type (actual, estimated, customer)
- **Bill** — Generated bills with line items, taxes, and status lifecycle
- **Payment** — Payment transactions with allocation to specific bills
- **Tariff** — Rate schedules with tiered pricing and seasonal adjustments

## Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- Oracle DB (or H2 for local dev)

### Run locally
```bash
# Uses H2 in-memory DB by default
./mvnw spring-boot:run

# With Oracle
./mvnw spring-boot:run -Dspring.profiles.active=oracle
```

### Run tests
```bash
./mvnw test
```

### Docker
```bash
docker build -t utility-billing-engine .
docker run -p 8080:8080 utility-billing-engine
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/meters/reads | Submit meter read |
| POST | /api/meters/reads/batch | Batch upload reads |
| GET | /api/meters/reads/{accountId} | Get reads for account |
| POST | /api/billing/run | Trigger billing run |
| GET | /api/billing/bills/{accountId} | Get bills for account |
| POST | /api/payments | Process payment |
| GET | /api/payments/{accountId} | Get payment history |
| GET | /api/accounts/{id} | Get account details |
| POST | /api/admin/scripts/reload | Reload Groovy scripts |

## Groovy Scripts

Place `.groovy` files in `src/main/resources/scripts/`:
- `residential-rate.groovy` — Residential tiered rate calculation
- Add custom scripts for commercial rates, demand charges, TOU pricing, etc.

Scripts receive a binding with `meterRead`, `account`, and `tariff` objects and return a calculated amount.
