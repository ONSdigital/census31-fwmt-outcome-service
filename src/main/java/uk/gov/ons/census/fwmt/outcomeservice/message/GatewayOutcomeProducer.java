package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;

@Slf4j
@Component
public class GatewayOutcomeProducer {

  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Retryable
  public boolean sendOutcome(String outcomeEvent, String transactionId) throws GatewayException {
    JsonNode envelope = parseEnvelope(outcomeEvent);
    JsonNode header = envelope.path("header");
    String topic = requiredHeaderValue(header, "topic", transactionId);
    String messageType = requiredHeaderValue(header, "messageType", transactionId);

    PubsubMessage message = PubsubMessage.newBuilder()
        .setData(ByteString.copyFromUtf8(outcomeEvent))
        .putAllAttributes(Map.of("contentType", "application/json"))
        .build();

    try {
      pubSubTemplate.publish(topic, message);
      log.info(
          "Published outcome gateway event operation={} messageType={} topic={} messageId={} correlationId={} caseId={} transactionId={}",
          publicationDetail().operation(),
          messageType,
          topic,
          header.path("messageId").asText(""),
          header.path("correlationId").asText(""),
          publicationDetail().caseId(),
          transactionId);
      return true;
    } catch (Exception e) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, e,
          "Cannot publish outcome for transaction ID " + transactionId
              + " messageType=" + messageType + " topic=" + topic);
    }
  }

  private JsonNode parseEnvelope(String outcomeEvent) throws GatewayException {
    try {
      JsonNode envelope = objectMapper.readTree(outcomeEvent);
      if (envelope == null || !envelope.isObject() || !envelope.path("header").isObject()) {
        throw new GatewayException(
            GatewayException.Fault.SYSTEM_ERROR,
            "Invalid Event Dictionary outcome event: header is missing");
      }
      return envelope;
    } catch (GatewayException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new GatewayException(
          GatewayException.Fault.SYSTEM_ERROR,
          "Invalid Event Dictionary outcome event: malformed JSON",
          exception);
    }
  }

  private String requiredHeaderValue(JsonNode header, String fieldName, String transactionId)
      throws GatewayException {
    String value = header.path(fieldName).asText("");
    if (value.isBlank()) {
      throw new GatewayException(
          GatewayException.Fault.SYSTEM_ERROR,
          "Invalid Event Dictionary outcome event: header." + fieldName
              + " is required for transaction ID " + transactionId);
    }
    return value;
  }

  public void logLegacyOutcomeSuppressed(
      String legacyDestination, String legacyEventType, String transactionId) {
    OutcomeOperationContext.Details detail = publicationDetail();
    log.error(
        "Legacy outcome suppressed legacy=true legacyDestination={} legacyEventType={} operation={} outcomeCode={} caseId={} surveyType={} transactionId={}",
        legacyDestination,
        legacyEventType,
        detail.operation(),
        detail.outcomeCode(),
        detail.caseId(),
        detail.surveyType(),
        transactionId);
  }

  private OutcomeOperationContext.Details publicationDetail() {
    OutcomeOperationContext.Details detail = OutcomeOperationContext.get();
    if (detail != null) {
      return detail;
    }
    return new OutcomeOperationContext.Details("UNKNOWN", "UNKNOWN", "UNKNOWN", "N/A", "UNKNOWN");
  }

}
