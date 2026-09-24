package uk.gov.ons.census.fwmt.outcomeservice.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EventDictionaryMessageFactoryTest {

  private final EventDictionaryMessageFactory factory = new EventDictionaryMessageFactory(new ObjectMapper());

  @Test
  void buildsRefusalReceivedEnvelope() throws Exception {
    String payload = factory.buildRefusalReceived("HARD_REFUSAL", "case-123", "agent-789");

    JsonNode root = new ObjectMapper().readTree(payload);
    assertThat(root.path("header").path("version").asText()).isEqualTo("1.0.0");
    assertThat(root.path("header").path("topic").asText()).isEqualTo("event_refusal-received");
    assertThat(root.path("header").path("source").asText()).isEqualTo("FIELDWORK_GATEWAY");
    assertThat(root.path("header").path("channel").asText()).isEqualTo("FIELD");
    assertThat(root.path("header").path("correlationId").asText()).isEmpty();
    assertThat(root.path("header").path("messageType").asText()).isEqualTo("REFUSAL_RECEIVED");
    assertThat(root.path("payload").path("refusal").path("type").asText()).isEqualTo("HARD_REFUSAL");
    assertThat(root.path("payload").path("refusal").path("agentId").asText()).isEqualTo("agent-789");
    assertThat(root.path("payload").path("refusal").path("collectionCase").path("id").asText())
        .isEqualTo("case-123");
  }

  @Test
  void buildsFulfilmentRequestWithConditionalContactFields() throws Exception {
    String payload = factory.buildFulfilmentRequest(
        "case-456",
        "P_OR_I1",
        "ind-222",
        true,
        "Ms",
        "Jo",
        "Smith",
        false,
        "+447700900123");

    JsonNode root = new ObjectMapper().readTree(payload);
    assertThat(root.path("header").path("topic").asText()).isEqualTo("event_fulfilment-request");
    assertThat(root.path("header").path("messageType").asText()).isEqualTo("FULFILMENT_REQUEST");
    assertThat(root.path("payload").path("fulfilmentRequest").path("fulfilmentCode").asText())
        .isEqualTo("P_OR_I1");
    assertThat(root.path("payload").path("fulfilmentRequest").path("caseId").asText())
        .isEqualTo("case-456");
    assertThat(root.path("payload").path("fulfilmentRequest").path("individualCaseId").asText())
        .isEqualTo("ind-222");
    assertThat(root.path("payload").path("fulfilmentRequest").path("contact").path("title").asText())
        .isEqualTo("Ms");
    assertThat(root.path("payload").path("fulfilmentRequest").path("contact").has("telNo")).isFalse();
  }
}