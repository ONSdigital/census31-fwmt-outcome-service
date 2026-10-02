package uk.gov.ons.census.fwmt.outcomeservice.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.fwmt.common.error.GatewayException;

@ExtendWith(OutputCaptureExtension.class)
class GatewayOutcomeProducerTest {

  private final PubSubTemplate pubSubTemplate = org.mockito.Mockito.mock(PubSubTemplate.class);

  private final GatewayOutcomeProducer producer = new GatewayOutcomeProducer();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(producer, "pubSubTemplate", pubSubTemplate);
    ReflectionTestUtils.setField(producer, "objectMapper", new ObjectMapper());
  }

  @Test
  void publishesRefusalEventsToDictionaryTopic() throws GatewayException {
    when(pubSubTemplate.publish(eq("event_refusal-received"), any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-1"));

    producer.sendOutcome(message("event_refusal-received", "REFUSAL_RECEIVED"), "tx-1");

    verify(pubSubTemplate).publish(eq("event_refusal-received"), any(PubsubMessage.class));
    verify(pubSubTemplate, never()).publish(eq("Field.other"), any(PubsubMessage.class));
  }

  @Test
  void publishesFieldCaseUpdatesToDictionaryTopic() throws GatewayException {
    when(pubSubTemplate.publish(eq("event_field-case-updated"), any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-2"));

    producer.sendOutcome(message("event_field-case-updated", "FIELD_CASE_UPDATED"), "tx-2");

    verify(pubSubTemplate).publish(eq("event_field-case-updated"), any(PubsubMessage.class));
    verify(pubSubTemplate, never()).publish(eq("Field.other"), any(PubsubMessage.class));
  }

  @Test
  void publishesFulfilmentRequestsToDictionaryTopic() throws GatewayException {
    when(pubSubTemplate.publish(eq("event_fulfilment-request"), any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-3"));

    producer.sendOutcome(message("event_fulfilment-request", "FULFILMENT_REQUEST"), "tx-3");

    verify(pubSubTemplate).publish(eq("event_fulfilment-request"), any(PubsubMessage.class));
    verify(pubSubTemplate, never()).publish(eq("Field.other"), any(PubsubMessage.class));
  }

  @Test
  void publishesAddressNotValidEventsToDictionaryTopic() throws GatewayException {
    when(pubSubTemplate.publish(eq("event_address-not-valid"), any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-4"));

    producer.sendOutcome(message("event_address-not-valid", "ADDRESS_NOT_VALID"), "tx-4");

    verify(pubSubTemplate).publish(eq("event_address-not-valid"), any(PubsubMessage.class));
    verify(pubSubTemplate, never()).publish(eq("Field.other"), any(PubsubMessage.class));
  }

  @Test
  void publishesQuestionnaireLinkedEventsToDictionaryTopic() throws GatewayException {
    when(pubSubTemplate.publish(eq("event_questionnaire-linked"), any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-5"));

    producer.sendOutcome(message("event_questionnaire-linked", "QUESTIONNAIRE_LINKED"), "tx-5");

    verify(pubSubTemplate).publish(eq("event_questionnaire-linked"), any(PubsubMessage.class));
    verify(pubSubTemplate, never()).publish(eq("Field.other"), any(PubsubMessage.class));
  }

  @Test
  void usesHeaderTopicRatherThanAnIndependentRoutingKey() throws GatewayException {
    String topic = "event_questionnaire-linked";
    producer.sendOutcome(message(topic, "QUESTIONNAIRE_LINKED"), "tx-topic");

    verify(pubSubTemplate).publish(eq(topic), any(PubsubMessage.class));
  }

  @Test
  void rejectsMalformedJsonWithoutPublishing() {
    assertThatThrownBy(() -> producer.sendOutcome("not-json", "tx-malformed"))
        .isInstanceOf(GatewayException.class)
        .hasMessageContaining("Invalid Event Dictionary outcome event");

    verify(pubSubTemplate, never()).publish(any(String.class), any(PubsubMessage.class));
  }

  @Test
  void rejectsMissingBlankTopicAndMissingMessageTypeWithoutPublishing() {
    for (String event : new String[] {
        "{\"header\":{\"messageType\":\"REFUSAL_RECEIVED\"}}",
        "{\"header\":{\"topic\":\"   \",\"messageType\":\"REFUSAL_RECEIVED\"}}",
        "{\"header\":{\"topic\":\"event_refusal-received\"}}"
    }) {
      assertThatThrownBy(() -> producer.sendOutcome(event, "tx-invalid"))
          .isInstanceOf(GatewayException.class);
    }

    verify(pubSubTemplate, never()).publish(any(String.class), any(PubsubMessage.class));
  }

  @Test
  void publishFailureRetainsGatewayExceptionAndEventContext() {
    String event = message("event_refusal-received", "REFUSAL_RECEIVED");
    when(pubSubTemplate.publish(eq("event_refusal-received"), any(PubsubMessage.class)))
        .thenThrow(new IllegalStateException("publish unavailable"));

    assertThatThrownBy(() -> producer.sendOutcome(event, "tx-publish"))
        .isInstanceOf(GatewayException.class)
        .hasMessageContaining("tx-publish")
        .hasMessageContaining("REFUSAL_RECEIVED")
        .hasMessageNotContaining("routingKey");
  }

  @Test
  void logsLegacySuppressionWithOutcomeContextWithoutPublishing(CapturedOutput output) {
    OutcomeOperationContext.set(
        new OutcomeOperationContext.Details("NEW_ADDRESS_REPORTED", "01-03-01", "tx-legacy", "case-1", "HH"));
    try {
      producer.logLegacyOutcomeSuppressed("Field.other", "NEW_ADDRESS_REPORTED", "tx-legacy");
    } finally {
      OutcomeOperationContext.clear();
    }

    assertThat(output.getOut())
        .contains("legacy=true")
        .contains("legacyDestination=Field.other")
        .contains("legacyEventType=NEW_ADDRESS_REPORTED")
        .contains("operation=NEW_ADDRESS_REPORTED")
        .contains("outcomeCode=01-03-01")
        .contains("caseId=case-1")
        .contains("surveyType=HH")
        .contains("transactionId=tx-legacy");
    verify(pubSubTemplate, never()).publish(any(String.class), any(PubsubMessage.class));
  }

  private String message(String topic, String messageType) {
    return "{\"header\":{\"topic\":\"" + topic + "\",\"messageType\":\"" + messageType
        + "\",\"messageId\":\"message-1\",\"correlationId\":\"correlation-1\"},\"payload\":{}}";
  }
}