# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**

## TODO list for documentation

- idempotency
- retries
- circuit breaker
- use MDC context to log request specific data such as payment id etc.
- only synchronous processing for simplicity
- add observability
- add tests for observability

## How to run the applications

Start the bank simulator using the Docker Desktop application or run the following command in the terminal:

```bash
docker-compose up
```

Then start running the `PaymentGatewayApplication` main method from your IDE.

This will start the payment gateway application on port `8089` and host `localhost`.

## Commands to test the payment gateway

After running the application, the swagger UI can be accessed at: **http://localhost:8090/swagger-ui/index.html**

Or you can use the following `curl` commands to test various scenarios:

```curl
curl -i -X POST http://localhost:8090/payments \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111",
    "expiry_month": 12,
    "expiry_year": 2025,
    "currency": "USD",
    "amount": 1000,
    "cvv": "123"
  }'
```

This will be rejected due to expired card.

```curl
curl -i -X POST http://localhost:8090/payments \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111",
    "expiry_month": 12,
    "expiry_year": 2026,
    "currency": "USD",
    "amount": 1000,
    "cvv": "123"
  }'
```

This will be successful with authorized status.

```curl
curl -i -X POST http://localhost:8090/payments \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111110",
    "expiry_month": 12,
    "expiry_year": 2026,
    "currency": "USD",
    "amount": 1000,
    "cvv": "123"
  }'
```

This will be rejected due to acquiring bank being unavailable.

```curl
curl -i -X POST http://localhost:8090/payments \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111112",
    "expiry_month": 12,
    "expiry_year": 2026,
    "currency": "USD",
    "amount": 1000,
    "cvv": "123"
  }'
```

This will be declined by acquiring bank.

To retrieve a specific payment with ID:

```curl
curl -i -X GET http://localhost:8090/payments/<PAYMENT_ID>
```

