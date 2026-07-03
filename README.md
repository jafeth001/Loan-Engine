# Loan Settlement & Prepayment Engine

Backend technical assessment — **Category A: Prepayment of Principal** with full infrastructure for MySQL and Kafka.

---

## Tech stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3 + Java 21 |
| Database | MySQL 8 + Spring Data JPA |
| Messaging | Apache Kafka 3.7 + Spring Kafka |
| Security | Spring Security + JWT |
| Containers | Docker Compose (MySQL, Redis, Kafka, Kafka UI, App) |
| Tests | JUnit 5, Spring Boot Test, Mockito |

---

## Architecture

- `controller/`
  - REST endpoints for loans, prepayments, auth, and audits
- `service/`
  - Business logic for loan creation, schedule generation, prepayment strategy execution, and auth
- `service/strategy/`
  - `PrepaymentStrategy` implementations for:
    - `REDUCE_EMI_KEEP_TENOR`
    - `REDUCE_TENOR_KEEP_EMI`
    - `ADVANCE_INSTALLMENTS`
- `config/`
  - Spring configuration for security, Redis cache, Kafka, and application behavior
- `security/`
  - JWT filter, authentication entry point, and related security classes
- `auth/`
  - Authentication controller, service, and DTOs
- `audit/`
  - Audit log domain, service, controller, repository, and DTOs
- `messaging/`
  - Kafka producers, consumers, and event definitions
- `domain/`
  - JPA entities: `Loan`, `LoanScheduleInstallment`, `LoanTransaction`, and enums
- `repository/`
  - Spring Data JPA repositories
- `dto/`
  - API request/response models and projection records
- `exception/`
  - Global exception handling and structured error responses

### Security model

| Endpoint | ROLE_ADMIN | ROLE_CUSTOMER |
|---|---|---|
| `POST /api/v1/auth/login` | ✅ | ✅ |
| `POST /api/v1/auth/register` | ✅ | ✅ |
| `GET /api/v1/loans/{loanId}` | ✅ | ✅ |
| `GET /api/v1/loans/{loanId}/schedule` | ✅ | ✅ |
| `POST /api/v1/loans` | ✅ | ❌ |
| `POST /api/v1/loans/{loanId}/prepayments` | ✅ | ❌ |
| `POST /api/v1/loans/{loanId}/mark-paid-up-to/{n}` | ✅ | ❌ |

### Kafka topics

| Topic | Published when | Key |
|---|---|---|
| `loan.created` | Loan created and persisted | `loanId` |
| `loan.prepayment.applied` | Prepayment successfully processed | `loanId` |

---

## Running locally

### Recommended: Docker Compose (full stack)

```bash
# Start infrastructure
docker compose up -d mysql kafka

# Start the application
docker compose up -d app

# Optional: Kafka UI
docker compose up -d kafka-ui
```

The backend runs on **`http://localhost:8080`**.

### Run locally against Docker infrastructure

```bash
docker compose up -d mysql kafka
mvn spring-boot:run
```

### Quick mode with H2

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

This uses in-memory H2 and disables Redis/Kafka.

### Run tests

```bash
mvn test
```

---

## API reference

### Authentication

#### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "username": "admin", "password": "adminPass1" }'
```

#### Register
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{ "username": "admin", "password": "adminPass1", "role": "ROLE_ADMIN" }'
```

Use the returned JWT as `Authorization: Bearer <token>` for protected endpoints.

---

### Loans

#### Create loan (ADMIN only)
```bash
curl -X POST http://localhost:8080/api/v1/loans \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "principalAmount": 1000000,
    "annualInterestRate": 12,
    "tenorMonths": 60,
    "startDate": "2024-01-01"
  }'
```

#### Get loan details + schedule
```bash
curl http://localhost:8080/api/v1/loans/1 \
  -H "Authorization: Bearer <token>"
```

#### Get loan schedule alias
```bash
curl http://localhost:8080/api/v1/loans/1/schedule \
  -H "Authorization: Bearer <token>"
```

#### Mark installments 1..N as paid (ADMIN only)
```bash
curl -X POST http://localhost:8080/api/v1/loans/1/mark-paid-up-to/23 \
  -H "Authorization: Bearer <token>"
```

---

### Prepayments

Use `installmentNumber` to select the prepayment trigger; the loan schedule is restructured according to the chosen option.

#### Option A — Reduce EMI, Keep Tenor
```bash
curl -X POST http://localhost:8080/api/v1/loans/1/prepayments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "installmentNumber": 24,
    "amount": 200000,
    "option": "REDUCE_EMI_KEEP_TENOR"
  }'
```

#### Option B — Reduce Tenor, Keep EMI
```bash
curl -X POST http://localhost:8080/api/v1/loans/1/prepayments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "installmentNumber": 24,
    "amount": 200000,
    "option": "REDUCE_TENOR_KEEP_EMI"
  }'
```

#### Option C — Advance Installments
```bash
curl -X POST http://localhost:8080/api/v1/loans/1/prepayments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "installmentNumber": 24,
    "amount": 200000,
    "option": "ADVANCE_INSTALLMENTS"
  }'
```

---

### Audits

#### Get all audits
```bash
curl http://localhost:8080/api/v1/audits \
  -H "Authorization: Bearer <token>"
```

#### Get audits for a loan
```bash
curl http://localhost:8080/api/v1/audits/loans/1 \
  -H "Authorization: Bearer <token>"
```

---

## Config values

| Property | Env var | Default |
|---|---|---|
| `JWT_SECRET` | — | Dev default in `application.yml` |
| `JWT_EXPIRATION_MS` | — | `3600000` |
| `KAFKA_BOOTSTRAP_SERVERS` | — | `localhost:9094` |

---

## Notes

- The API is designed for `ROLE_ADMIN` and `ROLE_CUSTOMER` access controls.
#   L o a n - E n g i n e 
 
 
