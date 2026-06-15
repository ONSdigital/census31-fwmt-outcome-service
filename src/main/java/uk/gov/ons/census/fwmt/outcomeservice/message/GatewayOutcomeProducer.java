package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class GatewayOutcomeProducer {

  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Value("${app.messaging.destinations.fieldRefusals:Field.refusals}")
  private String fieldRefusalsTopic;

  @Value("${app.messaging.destinations.fieldOther:Field.other}")
  private String fieldOtherTopic;

  @Retryable
  public void sendOutcome(String outcomeEvent, String transactionId, String routingKey) throws GatewayException {
    long epochMilli = Instant.now().toEpochMilli();

    PubsubMessage message = PubsubMessage.newBuilder()
        .setData(ByteString.copyFromUtf8(outcomeEvent))
        .putAllAttributes(
            Map.of(
                "contentType", "application/json",
                "routingKey", routingKey,
                "timestamp", String.valueOf(epochMilli)))
        .build();

    String topic = topicForRoutingKey(routingKey);
    try {
      pubSubTemplate.publish(topic, message);
      log.debug("Published outcome gateway event to Pub/Sub topic={} routingKey={}", topic, routingKey);
    } catch (Exception e) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, e,
          "Cannot publish outcome for transaction ID " + transactionId + " routingKey=" + routingKey);
    }
  }

  private String topicForRoutingKey(String routingKey) {
    if (GatewayOutcomeQueueConfig.GATEWAY_RESPONDENT_REFUSAL_ROUTING_KEY.equals(routingKey)) {
      return fieldRefusalsTopic;
    }
    if (GatewayOutcomeQueueConfig.GATEWAY_ADDRESS_UPDATE_ROUTING_KEY.equals(routingKey)
        || GatewayOutcomeQueueConfig.GATEWAY_FULFILMENT_REQUEST_ROUTING_KEY.equals(routingKey)
        || GatewayOutcomeQueueConfig.GATEWAY_QUESTIONNAIRE_UPDATE_ROUTING_KEY.equals(routingKey)
        || GatewayOutcomeQueueConfig.GATEWAY_FIELD_CASE_UPDATE_ROUTING_KEY.equals(routingKey)
        || GatewayOutcomeQueueConfig.GATEWAY_CCS_PROPERTY_LISTING_ROUTING_KEY.equals(routingKey)) {
      return fieldOtherTopic;
    }
    return GatewayOutcomeQueueConfig.GATEWAY_OUTCOME_EXCHANGE;
  }
}
