package uk.gov.ons.census.fwmt.outcomeservice.updateresidentcount;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.json.ParseException;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomeSetup;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.UpdateResidentCountProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.helpers.OutcomeHelper;
import uk.gov.ons.census.fwmt.outcomeservice.message.EventDictionaryMessageFactory;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCaseRecordService;
import uk.gov.ons.census.fwmt.outcomeservice.template.TemplateCreator;

import java.text.DateFormat;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateRessidentCountProcessorTest {

  @InjectMocks
  private UpdateResidentCountProcessor updateResidentCountProcessor;

  @Mock
  private GatewayCaseRecord gatewayCache;

  @Mock
  private GatewayEventManager eventManager;

  @Mock
  private TemplateCreator temmplateCreator;

  @Mock
  private DateFormat dateFormat;

  @Mock
  private Date date;

  @Mock
  private OutcomeSetup outcomeSetup;

  @Mock
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Mock
  private GatewayCaseRecordService gatewayCacheService;

  @Mock
  private EventDictionaryMessageFactory eventDictionaryMessageFactory;

  @Captor
  private ArgumentCaptor<GatewayCaseRecord> spiedCache;

  @Captor
  private ArgumentCaptor<String> outcomeEventCaptor;

  @Test
  @DisplayName("Should update the closed cache state to update")
  public void shouldUpdateTheClosedCacheStateToUpdate() throws GatewayException, ParseException, JSONException {
    final OutcomeSuperSetDto outcome = new OutcomeHelper().createUpdateResidentCount();
    when(eventDictionaryMessageFactory.buildFieldCaseUpdated(anyString(), any(Integer.class))).thenReturn("{}");
    GatewayCaseRecord gatewayCache = new GatewayCaseRecord();
    gatewayCache.setOriginalCaseId(outcome.getCaseId().toString());
    gatewayCache.setLastActionInstruction("CANCEL");
    when(gatewayCacheService.getById(anyString())).thenReturn(gatewayCache);
    updateResidentCountProcessor.process(outcome, outcome.getCaseId(), "CE");
    verify(gatewayCacheService).save(spiedCache.capture());
    String lastActionInstruction = spiedCache.getValue().lastActionInstruction;
    Assertions.assertEquals("UPDATE", lastActionInstruction);
  }

  @Test
  @DisplayName("Should emit Event Dictionary field case update payload")
  public void shouldEmitEventDictionaryFieldCaseUpdatePayload() throws Exception {
    final OutcomeSuperSetDto outcome = new OutcomeHelper().createUpdateResidentCount();
    when(eventDictionaryMessageFactory.buildFieldCaseUpdated(anyString(), any(Integer.class))).thenReturn(
      "{\"header\":{\"topic\":\"event_field-case-updated\",\"source\":\"FIELDWORK_GATEWAY\",\"channel\":\"FIELD\",\"messageType\":\"FIELD_CASE_UPDATED\"},\"payload\":{\"fieldCaseUpdate\":{\"caseId\":\""
        + outcome.getCaseId()
        + "\",\"ceExpectedCapacity\":5}}}");

    updateResidentCountProcessor.process(outcome, outcome.getCaseId(), "CE");

    verify(gatewayOutcomeProducer).sendOutcome(outcomeEventCaptor.capture(), any(), any());
    JsonNode root = new ObjectMapper().readTree(outcomeEventCaptor.getValue());

    Assertions.assertTrue(root.has("header"));
    Assertions.assertFalse(root.has("event"));
    Assertions.assertEquals("event_field-case-updated", root.path("header").path("topic").asText());
    Assertions.assertEquals("FIELDWORK_GATEWAY", root.path("header").path("source").asText());
    Assertions.assertEquals("FIELD", root.path("header").path("channel").asText());
    Assertions.assertEquals("FIELD_CASE_UPDATED", root.path("header").path("messageType").asText());
    Assertions.assertEquals(outcome.getCaseId().toString(),
        root.path("payload").path("fieldCaseUpdate").path("caseId").asText());
    Assertions.assertEquals(5,
        root.path("payload").path("fieldCaseUpdate").path("ceExpectedCapacity").asInt());
  }
}
