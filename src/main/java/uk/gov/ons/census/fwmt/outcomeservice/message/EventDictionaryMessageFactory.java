package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;

@Component
public class EventDictionaryMessageFactory {

  private final ObjectMapper objectMapper;

  public EventDictionaryMessageFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String buildRefusalReceived(String refusalType, String caseId, String agentId)
      throws GatewayException {
    Map<String, Object> refusal = new LinkedHashMap<>();
    refusal.put("type", refusalType);
    putIfHasText(refusal, "agentId", agentId);
    refusal.put("collectionCase", Map.of("id", caseId));
    return createMessage(
        GatewayOutcomeQueueConfig.EVENT_REFUSAL_RECEIVED_TOPIC,
        "REFUSAL_RECEIVED",
        Map.of("refusal", refusal));
  }

  public String buildFieldCaseUpdated(String caseId, int ceExpectedCapacity) throws GatewayException {
    Map<String, Object> fieldCaseUpdate = new LinkedHashMap<>();
    fieldCaseUpdate.put("caseId", caseId);
    fieldCaseUpdate.put("ceExpectedCapacity", ceExpectedCapacity);
    return createMessage(
        GatewayOutcomeQueueConfig.EVENT_FIELD_CASE_UPDATED_TOPIC,
        "FIELD_CASE_UPDATED",
        Map.of("fieldCaseUpdate", fieldCaseUpdate));
  }

        public String buildAddressNotValid(String reason, String caseId) throws GatewayException {
          Map<String, Object> invalidAddress = new LinkedHashMap<>();
          invalidAddress.put("reason", reason);
          invalidAddress.put("caseId", caseId);
          return createMessage(
          GatewayOutcomeQueueConfig.EVENT_ADDRESS_NOT_VALID_TOPIC,
          "ADDRESS_NOT_VALID",
          Map.of("invalidAddress", invalidAddress));
        }

  public String buildFulfilmentRequest(
      String caseId,
      String fulfilmentCode,
      String individualCaseId,
      boolean includeNameContact,
      String requesterTitle,
      String requesterForename,
      String requesterSurname,
      boolean includePhoneContact,
      String requesterPhone)
      throws GatewayException {
    Map<String, Object> fulfilmentRequest = new LinkedHashMap<>();
    fulfilmentRequest.put("fulfilmentCode", fulfilmentCode);
    fulfilmentRequest.put("caseId", caseId);
    putIfHasText(fulfilmentRequest, "individualCaseId", individualCaseId);

    Map<String, Object> contact = new LinkedHashMap<>();
    if (includeNameContact) {
      putIfHasText(contact, "title", requesterTitle);
      putIfHasText(contact, "forename", requesterForename);
      putIfHasText(contact, "surname", requesterSurname);
    }
    if (includePhoneContact) {
      putIfHasText(contact, "telNo", requesterPhone);
    }
    if (!contact.isEmpty()) {
      fulfilmentRequest.put("contact", contact);
    }

    return createMessage(
        GatewayOutcomeQueueConfig.EVENT_FULFILMENT_REQUEST_TOPIC,
        "FULFILMENT_REQUEST",
        Map.of("fulfilmentRequest", fulfilmentRequest));
  }

  private String createMessage(String topic, String messageType, Map<String, Object> payload)
      throws GatewayException {
    Map<String, Object> header = new LinkedHashMap<>();
    header.put("version", "1.0.0");
    header.put("topic", topic);
    header.put("source", "FIELDWORK_GATEWAY");
    header.put("channel", "FIELD");
    header.put("dateTime", Instant.now().toString());
    header.put("messageId", UUID.randomUUID().toString());
    header.put("correlationId", "");
    header.put("messageType", messageType);

    Map<String, Object> message = new LinkedHashMap<>();
    message.put("header", header);
    message.put("payload", payload);

    try {
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException exception) {
      throw new GatewayException(
          GatewayException.Fault.SYSTEM_ERROR,
          "Problem creating Event Dictionary outcome message",
          exception);
    }
  }

  private void putIfHasText(Map<String, Object> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }
}
