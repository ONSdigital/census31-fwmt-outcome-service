package uk.gov.ons.census.fwmt.outcomeservice.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Ensure Spring Cloud GCP Pub/Sub runs against emulator
 * without requiring Application Default Credentials.
 */
@Configuration
public class OutcomeServicePubSubEmulatorConfig {

  @Bean
  @Primary
  public CredentialsProvider pubsubNoCredentialsProvider() {
    return NoCredentialsProvider.create();
  }
}

