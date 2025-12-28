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

## Running the Applications

1) Start the bank simulator using Docker Desktop or via terminal:
```bash
docker-compose up
```

2) Start the Payment Gateway application from your IDE by running the `PaymentGatewayApplication` main method.
   - The application will start on `localhost:8089`

## Testing the Payment Gateway

### Swagger UI

Once the application is running, the Swagger UI can be accessed at:
http://localhost:8090/swagger-ui/index.html

### Using curl

Or you can use the following `curl` commands to test various scenarios:

Example 1: Successful payment
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

Example 2: Expired card (will be rejected)
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

Example 3: Payment rejected due to acquiring bank unavailable
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

Example 4: Payment declined by acquiring bank
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

Retrieve a payment by ID:
```curl
curl -i -X GET http://localhost:8090/payments/<PAYMENT_ID>
```
Replace `<PAYMENT_ID>` with the ID returned from the POST response.

### Notes on Current Implementation

- This solution meets the basic requirements of the challenge and keeps the implementation simple.
- Payments are processed synchronously.
- Payment records are stored in-memory; restarting the app clears the data.
- No idempotency, retries, or circuit breakers are implemented in this version.

## Possible Improvements (Production Considerations)

- Idempotency should be added to ensure that duplicate payment requests do not result in multiple charges:
Idempotency key should be provided by the client and stored with the payment record.
- Retries using exponential backoff and jitter should be implemented for transient errors when communicating with the acquiring bank.
- Circuit breakers should be added around calls to the acquiring bank to prevent cascading failures.
- Support for asynchronous processing using messaging queues to improve scalability and responsiveness.
- Use MDC context to log request specific data such as payment id etc.
- Add observability via metrics and tracing.
- Use a database for storing payment records instead of in-memory storage to ensure data persistence.
- Reason of rejected, declined payments should be stored for auditing and troubleshooting purposes.
- Authentication and authorization should be implemented to secure the API endpoints.
- Rate limiting should be added to prevent abuse of the payment API.
- Card number should be stored securely using tokenization to comply with PCI DSS standards:
Request should contain a token representing the card details instead of the raw card number.
- Add tests for observability.
- Resiliency testing should be performed to ensure the system can handle failures.
- Load testing should be conducted to validate performance under high traffic conditions.
