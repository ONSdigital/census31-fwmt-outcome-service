package uk.gov.ons.census.fwmt.outcomeservice.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.LinkedQidProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.dto.FulfilmentRequestDto;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.EventDictionaryMessageFactory;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCaseRecordService;

@ExtendWith(MockitoExtension.class)
class LinkedQidProcessorTest {

  @InjectMocks
  private LinkedQidProcessor processor;

  @Mock
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Mock
  private GatewayEventManager gatewayEventManager;

  @Mock
  private GatewayCaseRecordService gatewayCacheService;

  @Mock
  private EventDictionaryMessageFactory eventDictionaryMessageFactory;

  @Test
  void publishesQuestionnaireLinkedDictionaryMessageAndStoresDeliveredCase() throws GatewayException {
    UUID originalCaseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    UUID effectiveCaseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");
    UUID transactionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174003");
    OutcomeSuperSetDto outcome = new OutcomeSuperSetDto();
    outcome.setCaseId(originalCaseId);
    outcome.setTransactionId(transactionId);
    outcome.setFulfilmentRequests(
        List.of(FulfilmentRequestDto.builder().questionnaireID("qid-123").build()));
    when(eventDictionaryMessageFactory.buildQuestionnaireLinked("qid-123", effectiveCaseId.toString()))
        .thenReturn("{\"dictionary\":true}");

    processor.process(outcome, effectiveCaseId, "HH");

    verify(eventDictionaryMessageFactory)
        .buildQuestionnaireLinked("qid-123", effectiveCaseId.toString());
    verify(gatewayOutcomeProducer).sendOutcome(
        eq("{\"dictionary\":true}"),
        eq(transactionId.toString()),
        eq(GatewayOutcomeQueueConfig.GATEWAY_QUESTIONNAIRE_LINKED_ROUTING_KEY));
    ArgumentCaptor<GatewayCaseRecord> savedRecord = ArgumentCaptor.forClass(GatewayCaseRecord.class);
    verify(gatewayCacheService).save(savedRecord.capture());
    assertThat(savedRecord.getValue().isDelivered()).isTrue();
  }
}