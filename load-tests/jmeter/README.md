# JMeter load tests for CatchIt

This folder contains ready-to-run JMeter plans for your backend and payment-service.

## Files

- `catchit-checkout-flow.jmx`: End-to-end authenticated flow (`login -> cart -> checkout session -> confirm -> order validation`).
- `catchit-payment-service.jmx`: Isolated payment-service load (`POST /api/payments/authorize`).
- `users.csv`: Credentials and payment method IDs used by the checkout flow test.
- `register-users.ps1`: Optional helper to create users from `users.csv`.
- `run-load-tests.ps1`: Team-friendly runner with resource limits and profiles (`smoke`, `baseline`, `stress`).

## Quick run (simple)

This repo includes a dedicated compose file for load tests:

- `docker-compose.loadtest.yml`

It runs JMeter in separate containers with explicit CPU/RAM limits so local laptops are less likely to overheat.

### 1. Optional: set team defaults

Copy `load-tests/jmeter/.env.loadtest.example` to `.env` in repository root and adjust values if needed.

### 2. Start app services

```sh
docker compose up --build -d
```

### 3. Run checkout load test (isolated JMeter container)

```sh
docker compose -f docker-compose.loadtest.yml --profile checkout up --abort-on-container-exit --force-recreate jmeter-checkout
```

### 4. Run payment-service load test (isolated JMeter container)

```sh
docker compose -f docker-compose.loadtest.yml --profile payment up --abort-on-container-exit --force-recreate jmeter-payment
```

Result files:

- `load-tests/jmeter/results-checkout-compose.jtl`
- `load-tests/jmeter/results-payment-compose.jtl`

## Prerequisites

1. Services are up (from repository root):

```sh
docker compose up --build -d
```

2. No local JMeter install required (tests run in Docker containers).

3. Ensure users in `users.csv` exist in the backend.

## Create test users (optional)

If your test users do not exist yet, use your normal register endpoint flow before running load tests.

## Standardized team run

Use shared values in `.env` (based on `load-tests/jmeter/.env.loadtest.example`) so all team members run with the same limits.

## Recommended first execution profile

- Checkout flow: `threads=8`, `ramp=40`, `loops=10`
- Payment service: `threads=20`, `ramp=40`, `loops=20`

Then increase gradually (for example +25% each run).

## How to compare different PCs fairly

Do not compare max throughput from different laptops directly.

For fair comparison:

1. Use the same profile (for example `baseline`) and same dataset (`users.csv`).
2. Keep Dockerized JMeter limits equal (`--cpus` and `--memory`).
3. Compare percentile latency (p95/p99) and error rate, not only requests per second.
4. Prefer running final benchmark in one shared environment (CI runner or dedicated VM).

## Notes

- Checkout flow uses payment method IDs from `users.csv`; use values starting with `balance-` (for example `balance-main`).
- Backend endpoints under `/api/**` require JWT auth; this test extracts token from login and reuses it automatically.
- If you get many `401` responses, verify user credentials in `users.csv`.
- If you get many `402` on checkout confirm, reduce loops or item value, or increase user balance in seed/test data.

## Endpoints used by these tests

Checkout flow plan (`catchit-checkout-flow.jmx`):

- `POST /api/auth/login`
- `POST /api/cart/items`
- `GET /api/cart`
- `POST /api/checkout/session`
- `POST /api/checkout/confirm`
- `GET /api/checkout/orders/{orderId}/validation`

Payment-service plan (`catchit-payment-service.jmx`):

- `POST /api/payments/authorize`
