package uk.gov.ons.census.fwmt.outcomeservice.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.AddressTypeChangedCeEstProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.AddressTypeChangedHhProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.AddressTypeChangedSpgProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.InterviewRequiredCeProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.InterviewRequiredHhProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.NewAddressReportedProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.NewSplitAddressProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.NewUnitAddressLinkedProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.PropertyListedCeProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.PropertyListedHhProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.helpers.OutcomeHelper;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCaseRecordService;

@ExtendWith(MockitoExtension.class)
class LegacyOutcomeSuppressionProcessorFamiliesTest {

  @Mock
  private GatewayOutcomeProducer producer;

  @Mock
  private GatewayEventManager eventManager;

  @Mock
  private GatewayCaseRecordService cacheService;

  @Test
  void suppressesAllAddressTypeChangeVariantsAndPreservesCacheAndGeneratedIds() throws GatewayException {
    OutcomeSuperSetDto outcome = new OutcomeHelper().createNewStandaloneOutcome();
    String parentId = outcome.getCaseId().toString();
    when(cacheService.getById(anyString())).thenAnswer(invocation ->
        parentId.equals(invocation.getArgument(0))
            ? GatewayCaseRecord.builder().caseId(parentId).build()
            : null);

    for (OutcomeServiceProcessor processor : new OutcomeServiceProcessor[] {
        inject(new AddressTypeChangedCeEstProcessor()),
        inject(new AddressTypeChangedHhProcessor()),
        inject(new AddressTypeChangedSpgProcessor())
    }) {
      UUID newCaseId = processor.process(outcome, null, "HH");
      org.assertj.core.api.Assertions.assertThat(newCaseId).isNotEqualTo(outcome.getCaseId());
    }

    verify(producer, times(3)).logLegacyOutcomeSuppressed(
        eq("Field.other"), eq("ADDRESS_TYPE_CHANGED"), eq(outcome.getTransactionId().toString()));
    verify(cacheService, times(3)).save(any(GatewayCaseRecord.class));
    verify(producer, never()).sendOutcome(any(), any());
  }

  @Test
  void suppressesAllNewAddressVariantsAndPreservesTheirCaseIdsAndCaches() throws GatewayException {
    OutcomeSuperSetDto outcome = new OutcomeHelper().createNewStandaloneOutcome();
    when(cacheService.getById(anyString())).thenReturn(null);

    for (OutcomeServiceProcessor processor : new OutcomeServiceProcessor[] {
        inject(new NewAddressReportedProcessor()),
        inject(new NewSplitAddressProcessor()),
        inject(new NewUnitAddressLinkedProcessor())
    }) {
      processor.process(outcome, null, "CE");
    }

    verify(producer, times(3)).logLegacyOutcomeSuppressed(
        eq("Field.other"), eq("NEW_ADDRESS_REPORTED"), eq(outcome.getTransactionId().toString()));
    verify(cacheService, times(3)).save(any(GatewayCaseRecord.class));
    verify(producer, never()).sendOutcome(any(), any());
  }

  @Test
  void suppressesAllCcsAddressListedVariantsAndPreservesGeneratedCasesAndCaches() throws GatewayException {
    OutcomeSuperSetDto outcome = new OutcomeHelper().createNewStandaloneOutcome();

    for (OutcomeServiceProcessor processor : new OutcomeServiceProcessor[] {
        inject(new PropertyListedCeProcessor()),
        inject(new PropertyListedHhProcessor()),
        inject(new InterviewRequiredCeProcessor()),
        inject(new InterviewRequiredHhProcessor())
    }) {
      UUID newCaseId = processor.process(outcome, null, "HH");
      org.assertj.core.api.Assertions.assertThat(newCaseId).isNotEqualTo(outcome.getCaseId());
    }

    verify(producer, times(4)).logLegacyOutcomeSuppressed(
        eq("Field.other"), eq("CCS_ADDRESS_LISTED"), eq(outcome.getTransactionId().toString()));
    verify(cacheService, times(4)).save(any(GatewayCaseRecord.class));
    verify(eventManager, never()).triggerEvent(anyString(), eq("OUTCOME_SENT"), any());
    verify(producer, never()).sendOutcome(any(), any());
  }

  private <T> T inject(T processor) {
    ReflectionTestUtils.setField(processor, "gatewayOutcomeProducer", producer);
    ReflectionTestUtils.setField(processor, "gatewayEventManager", eventManager);
    ReflectionTestUtils.setField(processor, "gatewayCacheService", cacheService);
    return processor;
  }
}
