package uk.gov.ons.census.fwmt.outcomeservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.events.producer.GatewayEventProducer;
import uk.gov.ons.census.fwmt.events.producer.GatewayLoggingEventProducer;
import uk.gov.ons.census.fwmt.events.producer.PubSubGatewayEventProducer;
import uk.gov.ons.census.fwmt.outcomeservice.Application;

import java.util.Arrays;

@Slf4j
@Configuration
public class GatewayEventsConfig {

  @Value("${app.testing}")
  private boolean testing;

  @Bean
  public GatewayEventManager gatewayEventManager(
      GatewayLoggingEventProducer gatewayLoggingEventProducer,
      ObjectProvider<PubSubGatewayEventProducer> pubSubGatewayEventProducer) {

    final GatewayEventManager gatewayEventManager;
    if (testing) {
      log.warn("\n\n \t IMPORTANT - Test Mode: ON        \n \t\t Service is initiated in test mode which, this should not occur in production \n\n");
      GatewayEventProducer messagingProducer = pubSubGatewayEventProducer.getIfAvailable();
      if (messagingProducer == null) {
        throw new IllegalStateException("No PubSubGatewayEventProducer available for acceptance testing");
      }
      gatewayEventManager = new GatewayEventManager(Arrays.asList(gatewayLoggingEventProducer, messagingProducer));
    } else {
      log.warn("\n\n \t IMPORTANT - Test Mode: OFF   \n\n");
      gatewayEventManager = new GatewayEventManager(Arrays.asList(gatewayLoggingEventProducer));
    }

    gatewayEventManager.setSource(Application.APPLICATION_NAME);
    return gatewayEventManager;
  }
}
