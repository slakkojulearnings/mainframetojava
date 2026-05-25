# CardDemo Deployment Guide

## Local Development

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose (optional, for PostgreSQL)

### Quick Start

1. **Build**
   ```bash
   cd java
   mvn clean package -DskipTests
   ```

2. **Run with H2 (in-memory database)**
   ```bash
   cd java/online-programs
   mvn spring-boot:run
   ```

3. **Access API**
   - REST API: http://localhost:8080/carddemo
   - Swagger UI: http://localhost:8080/carddemo/swagger-ui.html
   - H2 Console: http://localhost:8080/carddemo/h2-console

## Docker Deployment

### Build & Run with PostgreSQL

```bash
# Build image
docker build -t carddemo-java:1.0 .

# Start services (PostgreSQL + API)
docker-compose up -d

# Verify services are running
docker-compose ps

# View logs
docker-compose logs -f api

# Stop services
docker-compose down
```

### Access Running Services

- **API:** http://localhost:8080/carddemo
- **Swagger:** http://localhost:8080/carddemo/swagger-ui.html
- **Actuator:** http://localhost:8080/carddemo/actuator
- **PostgreSQL:** localhost:5432 (user: carddemo, password: carddemo)

### Test API Endpoints

```bash
# Signon
curl -X POST http://localhost:8080/carddemo/api/signon \
  -H "Content-Type: application/json" \
  -d '{"userId":"USER0001","password":"password"}'

# Get Menu (requires valid session)
curl -X GET http://localhost:8080/carddemo/api/user/menu \
  -H "Cookie: JSESSIONID=<session-id>"

# Account View
curl -X GET http://localhost:8080/carddemo/api/account/12345678901 \
  -H "Cookie: JSESSIONID=<session-id>"
```

## Kubernetes Deployment

### Prerequisites
- kubectl configured
- Docker image pushed to registry (e.g., Docker Hub, ECR)

### Sample K8s Manifest

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: carddemo-config
data:
  application.properties: |
    spring.datasource.url=jdbc:postgresql://postgres:5432/carddemo
    spring.datasource.username=carddemo
    spring.datasource.password=carddemo
    spring.jpa.hibernate.ddl-auto=validate

---
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
        envFrom:
        - configMapRef:
            name: carddemo-config
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /carddemo/actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /carddemo/actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: carddemo-api-svc
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: carddemo
```

Deploy:
```bash
kubectl apply -f carddemo-k8s.yaml
kubectl get svc carddemo-api-svc
```

## Production Considerations

### Database
- Use managed PostgreSQL (AWS RDS, Azure Database, GCP Cloud SQL)
- Enable automated backups and point-in-time recovery
- Use SSL/TLS for database connections
- Consider read replicas for scaling reads

### API Server
- Use load balancer (AWS ALB, Azure Load Balancer, etc.)
- Enable auto-scaling based on CPU/memory
- Configure health checks and graceful shutdown
- Use container registry (ECR, ACR, GCR)

### Monitoring
- Enable Spring Boot Actuator metrics (/actuator/metrics)
- Push metrics to Prometheus/CloudWatch
- Set up log aggregation (CloudWatch Logs, ELK, Datadog)
- Create alerts for error rates, latency, database connection pool

### Security
- Use secrets manager for database credentials (AWS Secrets Manager, Vault)
- Enable HTTPS/TLS (ACM, Azure Key Vault)
- Restrict network access (security groups, network policies)
- Enable API rate limiting and DDoS protection
- Implement authentication token rotation

### Deployment Strategy
- Blue-green deployment for zero-downtime updates
- Canary deployments to catch issues early
- Automated rollback on health check failures

## Troubleshooting

### API won't start
```bash
# Check logs
docker-compose logs api

# Common issues:
# - Database not ready: wait for postgres healthcheck to pass
# - Port already in use: change ports in docker-compose.yml
# - Schema validation failed: check schema.sql syntax
```

### Database connection errors
```bash
# Test PostgreSQL connection
docker exec -it carddemo-postgres psql -U carddemo -d carddemo -c "SELECT 1"

# Check network connectivity
docker network ls
docker network inspect carddemo-network
```

### High API latency
```bash
# Check database query performance
docker logs carddemo-postgres | grep slow

# Check JVM heap usage
curl http://localhost:8080/carddemo/actuator/metrics/jvm.memory.used
```

## Migration from File-based (VSAM) to Database

### Data Migration Steps

1. **Export VSAM files to SQL**
   ```bash
   java -cp online-programs/target/*.jar \
     com.carddemo.migration.VsamToSqlExporter \
     --input-dir=./data \
     --output=./migration.sql
   ```

2. **Load into target database**
   ```bash
   psql -U carddemo -d carddemo < migration.sql
   ```

3. **Verify data integrity**
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
   - Deploy code with JPA repositories (Phase 11+)
   - Run acceptance tests
   - Monitor for issues
   - Keep VSAM files as backup for 30 days

## Support

For issues, see CLAUDE.md in the project root.
