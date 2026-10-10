# Rate Limiter API Gateway with Circuit Breaker

A Java Spring Boot learning project that demonstrates an API Gateway in front of a separate Backend Service. The Gateway applies per-client rate limiting, checks a Circuit Breaker before calling the Backend, forwards requests over HTTP, and returns rate-limit headers and structured JSON errors.

> **Project status:** This is a learning/demo project, not a production-ready gateway. Free and paid clients are currently simulated using the `X-Client-Id` request header. There is no real authentication, user database, or payment/subscription verification.

## Architecture

```text
Client
  |
  | GET /data + X-Client-Id
  v
API Gateway (Spring Boot, port 8081)
  |-- Rate Limiter (Token Bucket)
  |-- Circuit Breaker (CLOSED / OPEN / HALF_OPEN)
  |-- Backend URL and timeout configuration
  |-- Rate-limit response headers and JSON errors
  |
  | HTTP GET /backend/data
  v
Backend Service (Spring Boot, port 8082)
  |
  v
Response returned through the Gateway to the client
```

## Features

- **Token Bucket rate limiting:** Tracks a separate bucket per client ID.
- **Demo client plans:** Configurable Free, Paid, and default client settings.
- **HTTP response headers:** Successful Gateway responses include `X-RateLimit-Limit` and `X-RateLimit-Remaining`.
- **Circuit Breaker:** Uses `CLOSED`, `OPEN`, and `HALF_OPEN` states to stop repeated calls to an unhealthy Backend and try again after a cooldown.
- **Separate services:** The Gateway and Backend run as independent Spring Boot applications.
- **Configurable Backend URL and timeouts:** Backend address, connection timeout, and read timeout are configured in `application.properties`.
- **Structured errors:** The Gateway uses a common JSON error response for rate-limit and Backend availability errors.
- **Logging:** Backend request exceptions are logged by the Gateway.
- **Automated tests:** Unit tests cover Rate Limiter and Circuit Breaker behavior.

## Technologies

- Java 24 (the development environment used for this project)
- Spring Boot 4.1.1
- Maven Wrapper
- Spring Web / REST endpoints
- JUnit-based automated tests

## Project layout

The current local workspace has the two applications in sibling folders:

```text
D:\Rate_Limiter_gateway\
├── rate-limiter-gateway\   # API Gateway, port 8081
└── backend-service\        # Backend Service, port 8082
```

The Gateway's Maven project is `rate-limiter-gateway`; the separate Backend Maven project is `backend-service`. When preparing a public GitHub repository, ensure both projects are included in the repository layout so another developer can build and run the complete system from a clone.

## Configuration

### Gateway

File:

```text
rate-limiter-gateway/src/main/resources/application.properties
```

The relevant settings are expected to look like this (retain any other existing settings in the file):

```properties
spring.application.name=demo
server.port=8081

# Rate limits
rate-limit.free.capacity=5
rate-limit.free.refill-rate=0.083
rate-limit.paid.capacity=50
rate-limit.paid.refill-rate=0.83
rate-limit.default.capacity=5
rate-limit.default.refill-rate=0.083

# Demo client IDs
rate-limit.clients.free-user=free_user_1
rate-limit.clients.paid-user=paid_user_1

# Separate Backend
backend.service.base-url=http://localhost:8082
backend.service.connect-timeout-ms=2000
backend.service.read-timeout-ms=3000

# Circuit Breaker
circuit-breaker.failure-threshold=5
circuit-breaker.cooldown-ms=10000
```

### What the client configuration means

- `free_user_1` uses a bucket with capacity 5 and refill rate `0.083` tokens per second.
- `paid_user_1` uses a bucket with capacity 50 and refill rate `0.83` tokens per second.
- Any other client ID uses the default bucket with capacity 5 and refill rate `0.083` tokens per second.

The refill-rate values are approximate: `0.083` is about one token every 12 seconds, and `0.83` is about one token every 1.2 seconds. Tokens accumulate up to the bucket capacity.

**Security limitation:** `X-Client-Id` is supplied by the caller. A caller can claim to be `paid_user_1`, so this header is not secure authentication or proof of payment. A real system should authenticate clients and obtain plan information from a trusted source, such as a database or verified token.

## Run the applications

Run the Backend and Gateway in **two separate terminals**. Keep both running.

### 1. Start the Backend Service

```powershell
cd D:\Rate_Limiter_gateway\backend-service
.\mvnw.cmd spring-boot:run
```

The Backend should start on port `8082`.

### 2. Start the API Gateway

Open a second terminal:

```powershell
cd D:\Rate_Limiter_gateway\rate-limiter-gateway
.\mvnw.cmd spring-boot:run
```

The Gateway should start on port `8081`.

## API endpoints

### Backend health-by-response check

```http
GET http://localhost:8082/backend/data
```

Example command:

```powershell
curl.exe -i http://localhost:8082/backend/data
```

Expected body:

```text
Hello! Data received from Separate Backend Service.
```

### Gateway data endpoint

```http
GET http://localhost:8081/data
```

Example Free client request:

```powershell
curl.exe -i -H "X-Client-Id: free_user_1" http://localhost:8081/data
```

Example Paid client request:

```powershell
curl.exe -i -H "X-Client-Id: paid_user_1" http://localhost:8081/data
```

A successful Gateway response should include:

```text
X-RateLimit-Limit: 5
X-RateLimit-Remaining: <remaining tokens>
```

for the Free demo client, or a limit of `50` for the Paid demo client. The response body should be the Backend response.

### Check Circuit Breaker state

```http
GET http://localhost:8081/status
```

Example:

```powershell
curl.exe -i http://localhost:8081/status
```

The body reports the current Circuit Breaker state, such as `CLOSED`, `OPEN`, or `HALF_OPEN`.

### Manual failure endpoint

If retained in the current Gateway controller, `GET /test-failure` manually records a Circuit Breaker failure. This is a development/demo endpoint, not an endpoint that should be exposed publicly in a production system.

## Test rate limiting

Use a new client ID so that the test begins with a new bucket. Send the requests close together so the bucket does not refill a full token during the test:

```powershell
for ($i = 1; $i -le 6; $i++) {
    Write-Host "`n--- Request $i ---"
    curl.exe -i -H "X-Client-Id: readme_rate_test" http://localhost:8081/data
}
```

With the default capacity of 5, the first five rapid requests should normally be allowed and the sixth should return `429 Too Many Requests`. The Rate Limiter is time-based, so slow execution can allow tokens to refill.

## Test the Circuit Breaker manually

1. Start both applications.
2. Stop only the Backend process while leaving the Gateway running.
3. Send requests through `http://localhost:8081/data` to generate Backend connection failures.
4. Check the Gateway terminal and `/status` for the Circuit Breaker state.
5. Restart the Backend, wait for the configured cooldown, and send a trial request through the Gateway.

Expected behavior: repeated Backend failures open the circuit; after the cooldown, a permitted trial request can test recovery. A successful trial should close the circuit. Exact timing depends on the current implementation and when the cooldown starts.

## Run automated tests

From the Gateway project directory:

```powershell
cd D:\Rate_Limiter_gateway\rate-limiter-gateway
.\mvnw.cmd test
```

Maven prints the test totals and `BUILD SUCCESS` when the suite completes successfully. Run this command again after code changes; previously passing tests do not guarantee that later changes still pass.

## HTTP status codes

| Status | Meaning in this project |
|---|---|
| `200 OK` | Request passed the Rate Limiter and Circuit Breaker, and the Backend returned a response. |
| `429 Too Many Requests` | The client's bucket did not have a whole token available. |
| `503 Service Unavailable` | The Circuit Breaker blocked the request or the Backend call failed/timed out. |

The Gateway's structured error response includes fields such as `status`, `error`, `message`, and `timestamp`.

## Known limitations and future improvements

- Client identity and Free/Paid plans are currently simulated with caller-controlled header values; add authentication and trusted plan lookup before any real use.
- The services currently use local URLs and are intended for local development.
- Add comprehensive integration tests for the Gateway-to-Backend HTTP call, 429/503 JSON responses, timeouts, and Circuit Breaker recovery.
- Review logging and avoid exposing diagnostic details to clients.
- Tune rate limits and timeout values according to real workload requirements before deployment.
- Consolidate both applications into the intended GitHub repository layout before publishing, so a fresh clone contains everything required to run the demo.
