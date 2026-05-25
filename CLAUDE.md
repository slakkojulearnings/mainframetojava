# CardDemo COBOL-to-Java Migration — Claude Project Guide

## Project Overview

Complete Java port of AWS CardDemo mainframe COBOL/CICS/VSAM credit card application. Goal: functional parity with byte-equivalent file output and REST API replacement for terminal-based CICS transactions.

**Status:** Phases 1-14 complete (batch programs, online REST API, database migration, deployment)

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│ Spring Boot 3.2.0 Online REST API (Port 8080)          │
│ ├─ Signon Controller (authentication)                   │
│ ├─ Menu Controllers (navigation)                        │
│ ├─ 10 Business Controllers (CRUD operations)            │
│ └─ Spring Data JPA (PostgreSQL/H2)                      │
├─────────────────────────────────────────────────────────┤
│ Spring Batch Scheduled Jobs                             │
│ ├─ CBACT01C (Account activity report)                   │
│ ├─ CBACT02C (Activity detail report)                    │
│ ├─ CBCUS01C (Customer master update)                    │
│ ├─ CBTRN01C (Transaction posting)                       │
│ ├─ CBTRN02C (Transaction detail)                        │
│ ├─ CBTRN03C (New transaction creation)                  │
│ └─ CBSTM03A (Statement generation)                      │
└─────────────────────────────────────────────────────────┘
```

## Key Technologies

- **Framework:** Spring Boot 3.2.0, Spring Data JPA, Spring Batch
- **Database:** PostgreSQL (production) / H2 (testing)
- **Binary I/O:** Fixed-length record codec for VSAM → file export compatibility
- **Testing:** JUnit 5, MockMvc (integration), Testcontainers (database)
- **Build:** Maven 3.6+, Java 17+
- **Container:** Docker, Docker Compose

## Directory Structure

```
mainframetojava/
├── java/
│   ├── cobol-codec/              # COBOL binary codec (packed decimal, EBCDIC, etc.)
│   ├── vsam-io/                  # VSAM file I/O (KsdsReader, FixedRecordReader)
│   ├── batch-programs/           # Spring Batch jobs (CBACT*, CBCUS*, CBTRN*, CBSTM*)
│   ├── online-programs/          # Spring Boot REST API (12 controllers, 26 DTOs)
│   │   ├── src/main/java/com/carddemo/online/
│   │   │   ├── controller/       # REST endpoints
│   │   │   ├── dto/              # Request/response models
│   │   │   ├── repository/       # Spring Data JPA repositories
│   │   │   ├── entity/           # JPA entities (Account, Customer, Card, etc.)
│   │   │   └── service/          # Business logic layer
│   │   ├── src/test/java/        # 52+ integration tests
│   │   └── src/main/resources/   # application.properties, schema.sql
│   ├── verification/             # Byte-equivalence verification tests
│   └── pom.xml                   # Maven parent POM
├── docs/                         # Documentation (phases, analysis, ER diagrams)
├── scripts/                      # Utility scripts (data migration, setup)
└── CLAUDE.md                     # This file

```

## Key Files by Purpose

### Authentication & Session
- `CardDemoCommarea.java` — HTTP session state (userId, userType, context)
- `SignonController.java` — POST /api/signon with password validation
- `UserSecurityRecord.java` — USRSEC file POJO (80 bytes)

### Online Controllers (10/10 menu operations)
| Program | Endpoint | Method | Purpose |
|---------|----------|--------|---------|
| COACTVWC | /api/account/{id} | GET | Account view with 3-file cascade |
| COACUPDC | /api/account/{id}/update | POST | Update account status/limits |
| COCRDLSC | /api/cards | GET | List cards by account |
| COCRDVWC | /api/card/{num} | GET | Card detail (security-aware) |
| COCRDUPC | /api/card/{num}/update | POST | Update card status |
| COTRN02C | /api/transactions | GET | List transactions by card |
| COTRN01C | /api/transaction/{id} | GET | Transaction detail |
| COTRN03C | /api/transaction/add | POST | Add new transaction |
| CORPT01C | /api/report/account-statement/{id} | GET | Account statement with summary |
| COBIL00C | /api/bill-payment | POST | Process payment, update balance |

### Database Layer (Phase 11)
- `Account.java`, `Customer.java`, `Card.java`, `Transaction.java` — JPA entities
- `AccountRepository.java`, `CustomerRepository.java`, etc. — Spring Data interfaces
- `application.properties` — PostgreSQL/H2 connection config
- `schema.sql` — Database DDL (auto-applied on startup)

### Batch Programs (Phase 2-5, 11+)
- `CBACT01C.java`, `CBACT02C.java` — Activity reports
- `CBCUS01C.java` — Customer master update
- `CBTRN01C.java`, `CBTRN02C.java`, `CBTRN03C.java` — Transaction processing
- `CBSTM03A.java` — Statement generation

## Building & Running

### Prerequisites
```bash
# Java 17+
java -version

# Maven 3.6+
mvn -version

# PostgreSQL 13+ (optional, H2 used for testing)
psql --version
```

### Build
```bash
cd java
mvn clean package -DskipTests
```

### Run Online API (localhost:8080)
```bash
cd java/online-programs
mvn spring-boot:run
```

### Run Tests
```bash
# All tests
mvn clean test

# Online programs only
mvn test -pl online-programs

# Batch programs only
mvn test -pl batch-programs

# Verification (byte-equivalence vs COBOL)
mvn test -pl verification
```

### Run Batch Jobs (Spring Batch)
```bash
java -jar online-programs/target/online-programs-*.jar \
  --spring.batch.job.name=accountActivityJob \
  --input.file=./data/ACCTDAT.bin
```

### Docker
```bash
# Build image
docker build -f Dockerfile -t carddemo-java:1.0 .

# Run with PostgreSQL
docker-compose up -d
```

## Testing Strategy

### Integration Tests (52 tests, Phases 7-10)
- MockMvc for REST endpoint testing
- Temporary VSAM binary files via @DynamicPropertySource
- Session validation, input validation, error handling
- All tests use real Spring context, no mocking

### Repository Tests (Phase 11)
- JUnit 5 + Testcontainers (PostgreSQL in Docker)
- CRUD operations on real database
- Transaction rollback between tests

### Batch Tests
- Spring Batch test utilities
- File-based input/output verification
- Byte-equivalence checks vs original COBOL output

### Verification Tests (Phases 2-5)
- Binary-level comparison of generated files
- COBOL record layout validation
- Packed decimal, zoned decimal, EBCDIC round-trip testing

## Key Design Decisions

### 1. Session State (HTTP vs CICS)
**Decision:** Store commarea in Spring HttpSession, not thread-local or request-scoped
**Why:** Mimics CICS pseudo-conversational state; survives menu navigation; multiple tabs supported
**Trade-off:** Session must be passed to every request (cookie-based by default)

### 2. File I/O vs Database
**Phase 7-10:** VSAM file I/O via KsdsReader (sequential load into LinkedHashMap)
**Phase 11+:** PostgreSQL + JPA (transactional, queryable, indexable)
**Why:** VSAM files are write-heavy; database allows transactions, queries, scaling
**Migration:** Zero-downtime with dual-write pattern (file + DB), cutover when tested

### 3. Error Messages
**Decision:** Match COBOL error messages exactly (e.g., "Account Filter must be a non-zero 11 digit number")
**Why:** Simplifies UAT; eliminates surprises; eases cutover training
**Trade-off:** Error text change requires code change (not externalized)

### 4. BigDecimal for Money
**Decision:** All monetary fields use `BigDecimal` with `setScale(2, HALF_UP)`
**Why:** Exact decimal arithmetic (no floating-point rounding errors)
**Where:** AccountRecord.currBal, creditLimit; TransactionRecord.amount, etc.

### 5. Input Validation
**Decision:** Validate at HTTP boundary (controller), not at service/repository
**Why:** COBOL mirrors this pattern (screen validation before file I/O)
**Scope:** Type, range, format checks; business logic validation happens at service layer

## Data Migration (Phase 11)

### From VSAM to PostgreSQL

1. **Export VSAM to SQL INSERT statements**
   ```bash
   java -cp online-programs/target/*.jar \
     com.carddemo.migration.VsamToSqlExporter \
     --input-dir=./data \
     --output=./migration.sql
   ```

2. **Load into PostgreSQL**
   ```bash
   psql -U carddemo -d carddemo < migration.sql
   ```

3. **Verify counts**
   ```sql
   SELECT 'accounts' as table_name, COUNT(*) FROM account
   UNION ALL
   SELECT 'customers', COUNT(*) FROM customer
   UNION ALL
   SELECT 'cards', COUNT(*) FROM card
   UNION ALL
   SELECT 'transactions', COUNT(*) FROM transaction;
   ```

4. **Switchover**
   - Deploy Phase 11 code (JPA-based controllers)
   - Run acceptance tests against PostgreSQL
   - Keep VSAM files as backup for 30 days
   - Monitor for inconsistencies

## Deployment (Phase 14)

### Local Development
```bash
docker-compose up -d
mvn spring-boot:run
curl http://localhost:8080/carddemo/api/user/menu -H "Cookie: JSESSIONID=..."
```

### Docker Image
```dockerfile
FROM openjdk:17-slim
COPY online-programs/target/online-programs-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes (sample)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: carddemo-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: carddemo
  template:
    metadata:
      labels:
        app: carddemo
    spec:
      containers:
      - name: api
        image: carddemo-java:1.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres:5432/carddemo
        - name: SPRING_JPA_HIBERNATE_DDL_AUTO
          value: validate
```

## Monitoring & Observability

### Logging
- **Spring:** Configured in application.properties (logback)
- **Controllers:** Log request/response at WARN level
- **Batch:** Log job start/end, item count at INFO level
- **Database:** Log slow queries (>1s) at WARN level

### Metrics
- Spring Boot Actuator (/actuator/metrics)
- Micrometer for custom metrics (transaction count, account balance sum, etc.)
- Prometheus endpoint (/actuator/prometheus) for scraping

### Tracing
- Spring Cloud Sleuth for request correlation IDs
- Jaeger/Zipkin integration (optional, Phase 15+)

## Common Tasks

### Add a New REST Endpoint
1. Create DTO (request/response) in `dto/`
2. Create JPA entity (if new domain) in `entity/`
3. Create repository (if new domain) in `repository/`
4. Create controller method in appropriate `Controller.java`
5. Write integration test in `*Test.java`
6. Commit with clear message

### Add a New Batch Job
1. Create job configuration in `batch/config/`
2. Implement `ItemReader`, `ItemProcessor`, `ItemWriter`
3. Register in `@Configuration` class
4. Wire into `JobLauncher` (cron or manual)
5. Write test with `JobLauncherTestUtils`

### Fix a Bug
1. Reproduce with test case (unit or integration)
2. Fix the code
3. Verify test passes
4. Check no regressions: `mvn test`
5. Commit with reference to bug/issue

## Known Limitations

1. **VSAM files (Phase 7-10):** In-memory load via LinkedHashMap; OK for demo (50k records), not production
2. **File writes:** Naive rewrite-all pattern; should use RandomAccessFile for in-place updates
3. **Batch jobs:** Spring Batch; Spring Scheduling only; no distributed scheduling (Quartz, etc.)
4. **No caching:** All queries hit DB; no Redis/Memcached
5. **No async:** All endpoints synchronous; no reactive (Project Reactor)
6. **Session timeout:** Hardcoded 30m in application.properties

## Troubleshooting

### "Account not found" but I just created it
- Likely hitting old VSAM file; check `carddemo.acct.path` points to correct location
- Or using JPA but transaction didn't commit; check session not rolled back

### Test fails with "Address already in use :8080"
- Kill process: `lsof -i :8080 | grep -v PID | awk '{print $2}' | xargs kill -9`
- Or use different port: `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"`

### Batch job hangs
- Check log file for exceptions
- Verify input file exists and is readable
- If using DB, check connection pool not exhausted

### PostgreSQL connection refused
- Ensure PostgreSQL is running: `psql -U postgres -c "SELECT 1"`
- Check `spring.datasource.url` in application.properties
- For Docker: `docker-compose logs postgres`

## References

- **COBOL Source:** AWS CardDemo (https://github.com/aws-samples/aws-mainframe-modernization-carddemo)
- **Java Docs:** Spring Boot (3.2.0), Spring Data JPA, Spring Batch
- **Testing:** JUnit 5 guide, Testcontainers docs
- **Deployment:** Docker & Kubernetes best practices

## Contact & Support

This is an autonomous migration project. For questions:
1. Check this CLAUDE.md file first
2. Review inline code comments
3. Search test cases for examples
4. Check git log for decision history: `git log --oneline | grep -i phase`

---

**Last Updated:** May 25, 2026  
**Completed Phases:** 1-14 (batch programs, online REST API, database migration, deployment)  
**Next Phases:** Monitoring (Phase 15), Legacy integration (Phase 16), Front-end UI (Phase 17)
