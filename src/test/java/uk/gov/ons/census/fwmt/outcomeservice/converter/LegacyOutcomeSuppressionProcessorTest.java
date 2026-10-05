package uk.gov.ons.census.fwmt.outcomeservice.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.AddressTypeChangedHhProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.PropertyListedHhProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.helpers.OutcomeHelper;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCaseRecordService;

@ExtendWith(MockitoExtension.class)
class LegacyOutcomeSuppressionProcessorTest {

  @InjectMocks
  private AddressTypeChangedHhProcessor addressTypeChangedHhProcessor;

  @InjectMocks
  private PropertyListedHhProcessor propertyListedHhProcessor;

  @Mock
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Mock
  private GatewayEventManager gatewayEventManager;

  @Mock
  private GatewayCaseRecordService gatewayCaseRecordService;

  @Test
  void suppressesAddressTypeChangedAndPreservesTheGeneratedCaseAndCache() throws GatewayException {
    OutcomeSuperSetDto outcome = new OutcomeHelper().createNewStandaloneOutcome();
    UUID parentCaseId = outcome.getCaseId();
    when(gatewayCaseRecordService.getById(parentCaseId.toString()))
        .thenReturn(GatewayCaseRecord.builder().caseId(parentCaseId.toString()).build());

    UUID generatedCaseId = addressTypeChangedHhProcessor.process(outcome, null, "HH");

    assertThat(generatedCaseId).isNotEqualTo(parentCaseId);
    verify(gatewayCaseRecordService).save(any(GatewayCaseRecord.class));
    verify(gatewayOutcomeProducer).logLegacyOutcomeSuppressed(
        eq("Field.other"), eq("ADDRESS_TYPE_CHANGED"), eq(outcome.getTransactionId().toString()));
    verify(gatewayOutcomeProducer, never()).sendOutcome(any(), any());
    verify(gatewayEventManager, never())
      .triggerEvent(eq(parentCaseId.toString()), eq("OUTCOME_SENT"), any(String[].class));
  }

  @Test
  void suppressesCcsPropertyListedAndPreservesItsGeneratedCaseAndCache() throws GatewayException {
    OutcomeSuperSetDto outcome = new OutcomeHelper().createNewStandaloneOutcome();

    UUID generatedCaseId = propertyListedHhProcessor.process(outcome, null, "HH");

    assertThat(generatedCaseId).isNotEqualTo(outcome.getCaseId());
    verify(gatewayCaseRecordService).save(any(GatewayCaseRecord.class));
    verify(gatewayOutcomeProducer).logLegacyOutcomeSuppressed(
        eq("Field.other"), eq("CCS_ADDRESS_LISTED"), eq(outcome.getTransactionId().toString()));
    verify(gatewayOutcomeProducer, never()).sendOutcome(any(), any());
    verify(gatewayEventManager, never())
      .triggerEvent(eq(outcome.getCaseId().toString()), eq("OUTCOME_SENT"), any(String[].class));
  }
}
