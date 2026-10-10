package com.logancee.globalcart.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.function.Supplier;

@Configuration
public class SpannerConfig {

    @Bean
    @Primary
    public CredentialsProvider googleCredentials() {
        return FixedCredentialsProvider.create(NoCredentials.getInstance());
    }

    @Bean
    @Primary
    public DatabaseClient databaseClient(
            @Value("${spring.cloud.gcp.project-id:globalcart-dev-project}") String projectId,
            @Value("${spring.cloud.gcp.spanner.instance-id:globalcart-spanner-instance}") String instanceId,
            @Value("${spring.cloud.gcp.spanner.database:globalcart_db}") String databaseId,
            @Value("${spring.cloud.gcp.spanner.emulator-host:localhost:9010}") String emulatorHost
    ) {
        SpannerOptions.Builder builder = SpannerOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(NoCredentials.getInstance());

        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            builder.setEmulatorHost(emulatorHost);
        }

        Spanner spanner = builder.build().getService();
        DatabaseId dbId = DatabaseId.of(projectId, instanceId, databaseId);
        return spanner.getDatabaseClient(dbId);
    }

    @Bean
    @Primary
    public Supplier<DatabaseClient> databaseClientSupplier(DatabaseClient databaseClient) {
        return () -> databaseClient;
    }
}
