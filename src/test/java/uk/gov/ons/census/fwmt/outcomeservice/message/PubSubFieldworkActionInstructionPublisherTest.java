package uk.gov.ons.census.fwmt.outcomeservice.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.fwmt.common.rm.dto.ActionInstructionType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;

class PubSubFieldworkActionInstructionPublisherTest {

  @Test
  void publishesFlatInternalMessageWithStandardMetadata() {
    PubSubTemplate template = org.mockito.Mockito.mock(PubSubTemplate.class);
    when(template.publish(eq("event_fieldwork_action-instruction_internal"),
        org.mockito.ArgumentMatchers.any(PubsubMessage.class)))
        .thenReturn(CompletableFuture.completedFuture("message-123"));
    PubSubFieldworkActionInstructionPublisher publisher =
        new PubSubFieldworkActionInstructionPublisher(template, new ObjectMapper());
    ReflectionTestUtils.setField(publisher, "topic", "event_fieldwork_action-instruction_internal");

    FwmtActionInstruction payload = FwmtActionInstruction.builder()
        .actionInstruction(ActionInstructionType.UPDATE)
        .surveyName("CENSUS")
        .caseId("case-123")
        .build();
    publisher.publish(payload, new OutcomePublicationContext(
        "case-123", java.util.UUID.randomUUID(), Date.from(Instant.parse("2026-09-21T10:15:30Z")), ""));

    ArgumentCaptor<PubsubMessage> captured = ArgumentCaptor.forClass(PubsubMessage.class);
    verify(template).publish(eq("event_fieldwork_action-instruction_internal"), captured.capture());
    verify(template, never()).publish(eq("RM.Field"), any(PubsubMessage.class));
    verify(template, never()).publish(eq("GW.Field"), any(PubsubMessage.class));
    PubsubMessage message = captured.getValue();
    assertThat(message.getData().toStringUtf8()).contains("\"caseId\":\"case-123\"")
        .doesNotContain("__TypeId__", "timestamp");
    assertThat(message.getAttributesMap()).containsEntry("caseId", "case-123")
        .containsEntry("eventType", "FIELDWORK_ACTION_INSTRUCTION")
        .containsEntry("schemaVersion", "1.0")
        .containsEntry("correlationId", "")
        .containsEntry("occurredAt", "2026-09-21T10:15:30Z");
    assertThat(message.getAttributesOrDefault("eventId", "")).isNotEmpty();
  }
}