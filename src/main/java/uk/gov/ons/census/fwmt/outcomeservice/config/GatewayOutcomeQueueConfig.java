package uk.gov.ons.census.fwmt.outcomeservice.config;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayOutcomeQueueConfig {

  // Exchange name
  public static final String GATEWAY_OUTCOME_EXCHANGE = "events";

  public static final String LEGACY_FIELD_REFUSALS_TOPIC = "Field.refusals";
  public static final String LEGACY_FIELD_OTHER_TOPIC = "Field.other";

  public static final String EVENT_REFUSAL_RECEIVED_TOPIC = "event_refusal-received";
  public static final String EVENT_FIELD_CASE_UPDATED_TOPIC = "event_field-case-updated";
  public static final String EVENT_FULFILMENT_REQUEST_TOPIC = "event_fulfilment-request";
  public static final String EVENT_ADDRESS_NOT_VALID_TOPIC = "event_address-not-valid";
  public static final String EVENT_QUESTIONNAIRE_LINKED_TOPIC = "event_questionnaire-linked";

  // Routing keys
  public static final String GATEWAY_RESPONDENT_REFUSAL_ROUTING_KEY = "event.respondent.refusal";
  public static final String GATEWAY_ADDRESS_UPDATE_ROUTING_KEY = "event.case.address.update";
  public static final String GATEWAY_FULFILMENT_REQUEST_ROUTING_KEY = "event.fulfilment.request";
  public static final String GATEWAY_QUESTIONNAIRE_UPDATE_ROUTING_KEY = "event.questionnaire.update";
  public static final String GATEWAY_QUESTIONNAIRE_LINKED_ROUTING_KEY = "event.questionnaire-linked";
  public static final String GATEWAY_FIELD_CASE_UPDATE_ROUTING_KEY = "event.fieldcase.update";
  public static final String GATEWAY_CCS_PROPERTY_LISTING_ROUTING_KEY = "event.ccs.propertylisting";
    public static final String GATEWAY_ADDRESS_NOT_VALID_ROUTING_KEY = "event.address-not-valid";

  private static final Map<String, String> ROUTING_KEY_TO_TARGET_TOPIC = Map.of(
      GATEWAY_RESPONDENT_REFUSAL_ROUTING_KEY, EVENT_REFUSAL_RECEIVED_TOPIC,
      GATEWAY_FIELD_CASE_UPDATE_ROUTING_KEY, EVENT_FIELD_CASE_UPDATED_TOPIC,
      GATEWAY_FULFILMENT_REQUEST_ROUTING_KEY, EVENT_FULFILMENT_REQUEST_TOPIC,
      GATEWAY_ADDRESS_NOT_VALID_ROUTING_KEY, EVENT_ADDRESS_NOT_VALID_TOPIC,
      GATEWAY_QUESTIONNAIRE_LINKED_ROUTING_KEY, EVENT_QUESTIONNAIRE_LINKED_TOPIC);

  private static final Set<String> SUPPORTED_OPERATIONS = Set.of(
      "HARD_REFUSAL_RECEIVED",
      "EXTRAORDINARY_REFUSAL_RECEIVED",
      "UPDATE_RESIDENT_COUNT",
      "UPDATE_RESIDENT_COUNT_0",
      "UPDATE_RESIDENT_COUNT_1",
      "FULFILMENT_REQUESTED");

  private static final Set<String> LEGACY_REFUSAL_OPERATIONS = Set.of(
      "HARD_REFUSAL_RECEIVED",
      "EXTRAORDINARY_REFUSAL_RECEIVED");

  public static Optional<String> targetTopicForRoutingKey(String routingKey) {
    return Optional.ofNullable(ROUTING_KEY_TO_TARGET_TOPIC.get(routingKey));
  }

  public static boolean isSupportedOperation(String operation) {
    return SUPPORTED_OPERATIONS.contains(operation);
  }

  public static String retiredQueueForOperation(String operation) {
    if (LEGACY_REFUSAL_OPERATIONS.contains(operation)) {
      return LEGACY_FIELD_REFUSALS_TOPIC;
    }
    return LEGACY_FIELD_OTHER_TOPIC;
  }

  public static String retiredQueueForRoutingKey(String routingKey) {
    if (GATEWAY_RESPONDENT_REFUSAL_ROUTING_KEY.equals(routingKey)) {
      return LEGACY_FIELD_REFUSALS_TOPIC;
    }
    return LEGACY_FIELD_OTHER_TOPIC;
  }
}
