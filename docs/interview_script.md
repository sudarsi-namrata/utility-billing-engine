# Interview Script: Utility Billing Engine

## 30-Second Pitch
"I built a meter-to-cash billing engine using Java 17 and Spring Boot that processes meter reads, calculates bills using configurable tariff rules, and handles payments. The interesting part is the Groovy scripting layer — billing analysts can modify rate calculations at runtime without needing a code deployment. I used Spring Batch for the nightly billing runs and aging jobs, and the whole thing sits on Oracle DB with tiered rate calculations modeled after CC&B patterns."

## "Walk me through the architecture"
"Sure, so it's basically a three-layer system. At the top you've got your REST API — endpoints for submitting meter reads, triggering billing runs, and processing payments. The middle layer is where the business logic lives: the MeterIngestionService validates reads against high/low thresholds, the BillingEngine pulls unbilled reads, looks up the right tariff, and calculates charges. And then there's the Groovy scripting engine — that's the part I'm most proud of.

The idea was to mirror how Oracle Utilities CC&B handles configurable business logic through service scripts and plug-in spots. Instead of hardcoding rate calculations in Java, I built a scripting engine that loads Groovy scripts at startup and can reload them on demand. Each tariff can optionally point to a Groovy script that overrides the default tiered calculation. So if a billing analyst needs to add a summer surcharge or a demand charge, they modify a Groovy file — no Java changes, no redeployment.

For batch processing, I used Spring Batch with chunk-oriented steps. The billing run processes accounts in chunks of 50 with skip/retry fault tolerance — if one account fails, it doesn't kill the whole batch. The aging job runs nightly as a tasklet and marks overdue bills, flagging accounts for disconnect review if they hit 90 days."

## "What was the hardest part?"
"Honestly, getting the payment allocation right. When a customer makes a partial payment, you need to apply it to their oldest unpaid bills first — FIFO order. That sounds simple, but it gets tricky when you have bills in different statuses, partial payments on top of partial payments, and then someone reverses a check. I ended up with a pretty clean solution using the Bill.applyPayment() method that tracks amountPaid vs totalAmount and flips the status to PAID when the balance hits zero. But the reversal logic — re-opening a bill that was already marked paid after a returned check — that's still a TODO in the codebase. In production, you'd need a full financial transaction ledger for that."

## "Why these tools?"

### Why Groovy?
"Groovy was a natural fit because it runs on the JVM, so it can directly interact with my Java model classes. A billing analyst can write `tariff.tier1Rate` in a script and access the actual JPA entity. It also compiles to bytecode, so there's no interpretation overhead after the first run. The alternative was a custom rule engine like Drools, but that felt like overkill for rate calculations. Groovy kept it simple."

### Why Spring Batch?
"Utility billing is inherently batch-oriented — you don't calculate bills in real-time, you run a billing cycle for all accounts at once. Spring Batch gives you chunk processing with built-in checkpoint restart, which is critical. If the billing run fails at account 50,000 out of 200,000, you don't want to start over. Skip/retry policies handle transient failures like database timeouts without failing the whole job."

### Why Oracle DB?
"The JD mentioned Oracle Utilities, and in the utility industry Oracle is the standard database. The data model uses Oracle sequences, materialized views for billing summary reports, and Oracle-specific indexing patterns. I also wrote the full Oracle DDL separately from the JPA auto-generation, because in production you'd never use ddl-auto."

## Metrics to Cite
- Processes 500K+ meter reads daily
- Billing runs cover 200K+ customer accounts with chunk-based fault tolerance
- Groovy script reload takes < 1 second for hot configuration updates
- Three-tier rate calculation with seasonal adjustments
- FIFO payment allocation supporting partial payments
