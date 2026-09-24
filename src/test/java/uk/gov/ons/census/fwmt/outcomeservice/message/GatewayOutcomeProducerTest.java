package uk.gov.ons.census.fwmt.outcomeservice.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;

class GatewayOutcomeProducerTest {

  private final PubSubTemplate pubSubTemplate = org.mockito.Mockito.mock(PubSubTemplate.class);

  private final GatewayOutcomeProducer producer = new GatewayOutcomeProducer();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(producer, "pubSubTemplate", pubSubTemplate);
    setFieldIfPresent("fieldRefusalsTopic", "Field.refusals");
    setFieldIfPresent("fieldOtherTopic", "Field.other");
    setFieldIfPresent("refusalReceivedTopic", "event_refusal-received");
    setFieldIfPresent("fieldCaseUpdatedTopic", "event_field-case-updated");
    setFieldIfPresent("fulfilmentRequestTopic", "event_fulfilment-request");
    setFieldIfPresent("objectMapper", new ObjectMapper());
  }

  @Test
  void publishesRefusalEventsToDictionaryTopic() throws GatewayException {
    when(pubSubTemplate.publish(eq("event_refusal-received"), any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-1"));

    producer.sendOutcome("{}", "tx-1", GatewayOutcomeQueueConfig.GATEWAY_RESPONDENT_REFUSAL_ROUTING_KEY);

    verify(pubSubTemplate).publish(eq("event_refusal-received"), any(PubsubMessage.class));
    verify(pubSubTemplate, never()).publish(eq("Field.refusals"), any(PubsubMessage.class));
  }

  @Test
  void publishesFieldCaseUpdatesToDictionaryTopic() throws GatewayException {
    when(pubSubTemplate.publish(eq("event_field-case-updated"), any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-2"));

    producer.sendOutcome("{}", "tx-2", GatewayOutcomeQueueConfig.GATEWAY_FIELD_CASE_UPDATE_ROUTING_KEY);

    verify(pubSubTemplate).publish(eq("event_field-case-updated"), any(PubsubMessage.class));
    verify(pubSubTemplate, never()).publish(eq("Field.other"), any(PubsubMessage.class));
  }

  @Test
  void publishesFulfilmentRequestsToDictionaryTopic() throws GatewayException {
    when(pubSubTemplate.publish(eq("event_fulfilment-request"), any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-3"));

    producer.sendOutcome("{}", "tx-3", GatewayOutcomeQueueConfig.GATEWAY_FULFILMENT_REQUEST_ROUTING_KEY);

    verify(pubSubTemplate).publish(eq("event_fulfilment-request"), any(PubsubMessage.class));
    verify(pubSubTemplate, never()).publish(eq("Field.other"), any(PubsubMessage.class));
  }

  private void setFieldIfPresent(String fieldName, Object value) {
    for (Field field : producer.getClass().getDeclaredFields()) {
      if (field.getName().equals(fieldName)) {
        ReflectionTestUtils.setField(producer, fieldName, value);
        return;
      }
    }
  }
}