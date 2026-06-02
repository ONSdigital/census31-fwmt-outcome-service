package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.messaging.MessagingProperties;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class GatewayOutcomeProducer {

  @Autowired
  @Qualifier("OS_RT_RM")
  private ObjectProvider<RabbitTemplate> rabbitTemplate;

  @Autowired(required = false)
  private ObjectProvider<PubSubTemplate> pubSubTemplate;

  @Value("${app.messaging.provider:rabbit}")
  private String messagingProvider;

  @Value("${app.messaging.destinations.fieldRefusals:Field.refusals}")
  private String fieldRefusalsTopic;

  @Value("${app.messaging.destinations.fieldOther:Field.other}")
  private String fieldOtherTopic;

  @Autowired
  private ObjectMapper objectMapper;

  @Retryable
  public void sendOutcome(String outcomeEvent, String transactionId, String routingKey) throws GatewayException {
    if (MessagingProperties.PROVIDER_PUBSUB.equalsIgnoreCase(messagingProvider)) {
      publishToPubSub(outcomeEvent, transactionId, routingKey);
      return;
    }

    MessageProperties messageProperties = new MessageProperties();
    messageProperties.setContentType("application/json");
    long epochMilli = Instant.now().toEpochMilli();
    messageProperties.setTimestamp(new Date(epochMilli));
    MessageConverter messageConverter = new Jackson2JsonMessageConverter();

    objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(),
        true);

    try {
      Message message = messageConverter.toMessage(objectMapper.readTree(outcomeEvent), messageProperties);

      RabbitTemplate template = rabbitTemplate.getIfAvailable();
      if (template == null) {
        throw new GatewayException(
            GatewayException.Fault.SYSTEM_ERROR,
            "Rabbit is configured but OS_RT_RM RabbitTemplate is not available");
      }
      template.convertAndSend(GatewayOutcomeQueueConfig.GATEWAY_OUTCOME_EXCHANGE, routingKey, message);
    } catch (IOException e) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, e,
          "Cannot process address update for transaction ID " + transactionId + "msg: " + outcomeEvent);
    }
  }

  private void publishToPubSub(String outcomeEvent, String transactionId, String routingKey) throws GatewayException {
    PubSubTemplate template = pubSubTemplate.getIfAvailable();
    if (template == null) {
      throw new GatewayException(
          GatewayException.Fault.SYSTEM_ERROR,
          "Pub/Sub is configured but no PubSubTemplate is available (routingKey=" + routingKey + ")");
    }

    String topic = topicForRoutingKey(routingKey);
    long epochMilli = Instant.now().toEpochMilli();

    PubsubMessage message = PubsubMessage.newBuilder()
        .setData(ByteString.copyFromUtf8(outcomeEvent))
        .putAllAttributes(
            Map.of(
                "contentType", "application/json",
                "routingKey", routingKey,
                "timestamp", String.valueOf(epochMilli)))
        .build();

    try {
      template.publish(topic, message);
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
