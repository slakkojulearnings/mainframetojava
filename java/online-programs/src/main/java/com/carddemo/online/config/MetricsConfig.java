package com.carddemo.online.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public MetricsConfig(MeterRegistry meterRegistry) {
        io.micrometer.core.instrument.Counter.builder("carddemo.api.requests.total")
            .description("Total number of API requests")
            .register(meterRegistry);

        io.micrometer.core.instrument.Counter.builder("carddemo.api.errors.total")
            .description("Total number of API errors")
            .register(meterRegistry);

        io.micrometer.core.instrument.Gauge.builder("carddemo.database.accounts.count",
            () -> 0L)
            .description("Number of accounts in database")
            .register(meterRegistry);

        io.micrometer.core.instrument.Gauge.builder("carddemo.database.transactions.count",
            () -> 0L)
            .description("Number of transactions in database")
            .register(meterRegistry);
    }
}
