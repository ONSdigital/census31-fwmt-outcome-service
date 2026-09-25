package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;

@Slf4j
@Component
public class GatewayOutcomeProducer {

  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${app.messaging.destinations.refusalReceived:event_refusal-received}")
  private String refusalReceivedTopic;

  @Value("${app.messaging.destinations.fieldCaseUpdated:event_field-case-updated}")
  private String fieldCaseUpdatedTopic;

  @Value("${app.messaging.destinations.fulfilmentRequest:event_fulfilment-request}")
  private String fulfilmentRequestTopic;

  @Value("${app.messaging.destinations.addressNotValid:event_address-not-valid}")
  private String addressNotValidTopic;

  @Retryable
  public boolean sendOutcome(String outcomeEvent, String transactionId, String routingKey) throws GatewayException {
    String topic = topicForRoutingKey(routingKey);
    if (topic == null) {
      logSuppressedPublication(transactionId, routingKey);
      return false;
    }

    PubsubMessage message = PubsubMessage.newBuilder()
        .setData(ByteString.copyFromUtf8(outcomeEvent))
        .putAllAttributes(Map.of("contentType", "application/json"))
        .build();

    try {
      pubSubTemplate.publish(topic, message);
      log.info(
          "Published outcome gateway event operation={} messageType={} topic={} messageId={} correlationId={} caseId={} transactionId={}",
          publicationDetail().operation(),
          extractHeaderField(outcomeEvent, "messageType"),
          topic,
          extractHeaderField(outcomeEvent, "messageId"),
          extractHeaderField(outcomeEvent, "correlationId"),
          publicationDetail().caseId(),
          transactionId);
      return true;
    } catch (Exception e) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, e,
          "Cannot publish outcome for transaction ID " + transactionId + " routingKey=" + routingKey);
    }
  }

  private String topicForRoutingKey(String routingKey) {
    if (GatewayOutcomeQueueConfig.GATEWAY_RESPONDENT_REFUSAL_ROUTING_KEY.equals(routingKey)) {
      return refusalReceivedTopic;
    }
    if (GatewayOutcomeQueueConfig.GATEWAY_FIELD_CASE_UPDATE_ROUTING_KEY.equals(routingKey)) {
      return fieldCaseUpdatedTopic;
    }
    if (GatewayOutcomeQueueConfig.GATEWAY_FULFILMENT_REQUEST_ROUTING_KEY.equals(routingKey)) {
      return fulfilmentRequestTopic;
    }
    if (GatewayOutcomeQueueConfig.GATEWAY_ADDRESS_NOT_VALID_ROUTING_KEY.equals(routingKey)) {
      return addressNotValidTopic;
    }
    return null;
  }

  private void logSuppressedPublication(String transactionId, String routingKey) {
    OutcomeOperationContext.Details detail = publicationDetail();
    log.error(
        "Legacy queue {} is retired; operation {} has no replacement topic; event not published transactionId={} outcomeCode={} caseId={} routingKey={}",
        GatewayOutcomeQueueConfig.retiredQueueForRoutingKey(routingKey),
        detail.operation(),
        transactionId,
        detail.outcomeCode(),
        detail.caseId(),
        routingKey);
  }

  private OutcomeOperationContext.Details publicationDetail() {
    OutcomeOperationContext.Details detail = OutcomeOperationContext.get();
    if (detail != null) {
      return detail;
    }
    return new OutcomeOperationContext.Details("UNKNOWN", "UNKNOWN", "UNKNOWN", "N/A", "UNKNOWN");
  }

  private String extractHeaderField(String outcomeEvent, String fieldName) {
    try {
      JsonNode root = objectMapper.readTree(outcomeEvent);
      return root.path("header").path(fieldName).asText("");
    } catch (Exception exception) {
      log.warn("Unable to read outcome header field={} for publication log", fieldName, exception);
      return "";
    }
  }
}
