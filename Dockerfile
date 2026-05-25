FROM openjdk:17-slim

WORKDIR /app

COPY java/online-programs/target/online-programs-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/carddemo/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar", \
    "--spring.datasource.url=jdbc:postgresql://postgres:5432/carddemo", \
    "--spring.datasource.username=carddemo", \
    "--spring.datasource.password=carddemo", \
    "--spring.jpa.hibernate.ddl-auto=validate"]
