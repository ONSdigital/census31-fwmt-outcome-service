package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtCancelActionInstruction;

@Slf4j
@Service
public class PubSubFieldworkActionInstructionPublisher {

  private final PubSubTemplate pubSubTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.messaging.destinations.fieldworkActionInstructionInternal:event_fieldwork_action-instruction_internal}")
  private String topic;

  @Value("${app.messaging.publish-timeout-millis:5000}")
  private long publishTimeoutMillis;

  public PubSubFieldworkActionInstructionPublisher(PubSubTemplate pubSubTemplate, ObjectMapper objectMapper) {
    this.pubSubTemplate = pubSubTemplate;
    this.objectMapper = objectMapper;
  }

  public void publish(FwmtActionInstruction payload, OutcomePublicationContext context) {
    publishInstruction(payload, context);
  }

  public void publish(FwmtCancelActionInstruction payload, OutcomePublicationContext context) {
    publishInstruction(payload, context);
  }

  private void publishInstruction(Object payload, OutcomePublicationContext context) {
    String payloadCaseId = payload instanceof FwmtActionInstruction action
        ? action.getCaseId() : ((FwmtCancelActionInstruction) payload).getCaseId();
    if (!context.caseId().equals(payloadCaseId)) {
      throw new IllegalArgumentException("Publication context caseId does not match payload caseId");
    }

    String eventId = UUID.randomUUID().toString();
    final String body;
    try {
      body = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize action instruction " + eventId, exception);
    }

    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("eventId", eventId);
    attributes.put("correlationId", context.correlationId() == null ? "" : context.correlationId());
    attributes.put("caseId", payloadCaseId);
    attributes.put("eventType", "FIELDWORK_ACTION_INSTRUCTION");
    attributes.put("schemaVersion", "1.0");
    attributes.put("occurredAt", occurredAt(context.eventDate(), eventId));

    CompletableFuture<String> publishFuture = pubSubTemplate.publish(topic,
        PubsubMessage.newBuilder()
            .setData(ByteString.copyFromUtf8(body))
            .putAllAttributes(attributes)
            .build());
    try {
      String messageId = publishFuture.get(publishTimeoutMillis, TimeUnit.MILLISECONDS);
      log.info("Published fieldwork action instruction eventId={} transactionId={} messageId={}",
          eventId, context.transactionId(), messageId);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing action instruction " + eventId, exception);
    } catch (TimeoutException | ExecutionException exception) {
      throw new IllegalStateException("Unable to publish action instruction " + eventId, exception);
    }
  }

  private String occurredAt(Date eventDate, String eventId) {
    if (eventDate == null) {
      log.warn("Outcome eventDate is absent; using publish time eventId={}", eventId);
      return Instant.now().toString();
    }
    return eventDate.toInstant().toString();
  }
}