package uk.gov.ons.census.fwmt.outcomeservice.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayOutcomeQueueConfig {

  public static final String EVENT_REFUSAL_RECEIVED_TOPIC = "event_refusal-received";
  public static final String EVENT_FIELD_CASE_UPDATED_TOPIC = "event_field-case-updated";
  public static final String EVENT_FULFILMENT_REQUEST_TOPIC = "event_fulfilment-request";
  public static final String EVENT_ADDRESS_NOT_VALID_TOPIC = "event_address-not-valid";
  public static final String EVENT_QUESTIONNAIRE_LINKED_TOPIC = "event_questionnaire-linked";

}
