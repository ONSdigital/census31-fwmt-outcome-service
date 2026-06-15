package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingJsonCodec;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingMessageDispatcher;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OutcomePreprocessingPubSubConfig {

  @Value("${app.messaging.pubsub.outcome-preprocessing-subscription:outcome-service-Outcome-Preprocessing}")
  private String outcomePreprocessingSubscription;

  @Bean(name = "outcomePreprocessingPubSubInputChannel")
  public MessageChannel outcomePreprocessingPubSubInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter outcomePreprocessingPubSubInbound(
      @Qualifier("outcomePreprocessingPubSubInputChannel") MessageChannel inputChannel,
      PubSubTemplate pubSubTemplate) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, outcomePreprocessingSubscription);
    adapter.setOutputChannel(inputChannel);
    adapter.setAckMode(AckMode.MANUAL);
    return adapter;
  }

  @Bean
  @ServiceActivator(inputChannel = "outcomePreprocessingPubSubInputChannel")
  public MessageHandler outcomePreprocessingPubSubHandler(
      OutcomePreprocessingJsonCodec codec,
      OutcomePreprocessingMessageDispatcher dispatcher) {
    return message -> {
      BasicAcknowledgeablePubsubMessage original =
          message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
      PubsubMessage pubsubMessage = original.getPubsubMessage();
      try {
        Object payload = codec.fromPubsubMessage(pubsubMessage);
        dispatcher.dispatch(payload);
        original.ack();
      } catch (GatewayException ex) {
        log.error("Failed to process Outcome.Preprocessing Pub/Sub message", ex);
        original.nack();
      } catch (RuntimeException ex) {
        log.error("Failed to process Outcome.Preprocessing Pub/Sub message", ex);
        original.nack();
        throw ex;
      }
    };
  }
}
