package uk.gov.ons.census.fwmt.outcomeservice.newaddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.NewAddressReportedProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.helpers.OutcomeHelper;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCaseRecordService;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class NewAddressReportedTemplateTest {

  @InjectMocks
  private NewAddressReportedProcessor newAddressReportedProcessor;

  @Mock
  private GatewayCaseRecordService cacheService;

  @Mock
  private GatewayEventManager eventManager;

  @Mock
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Test
  void shouldSuppressLegacyEventAndKeepCacheAndCaseIdForSpg() throws GatewayException {
    final OutcomeSuperSetDto outcome = new OutcomeHelper().createNewStandaloneOutcome();
    UUID result = newAddressReportedProcessor.process(outcome, outcome.getCaseId(), "SPG");

    verify(gatewayOutcomeProducer).logLegacyOutcomeSuppressed(
        eq("Field.other"), eq("NEW_ADDRESS_REPORTED"), eq(String.valueOf(outcome.getTransactionId())));
    verify(gatewayOutcomeProducer, never()).sendOutcome(any(), any());
    verify(cacheService).save(any(GatewayCaseRecord.class));
    verify(eventManager, never())
      .triggerEvent(eq(outcome.getCaseId().toString()), eq("OUTCOME_SENT"), any(String[].class));
    org.assertj.core.api.Assertions.assertThat(result).isEqualTo(outcome.getCaseId());
  }

  @Test
  void shouldSuppressLegacyEventAndKeepCacheAndCaseIdForCe() throws GatewayException {
    final OutcomeSuperSetDto outcome = new OutcomeHelper().createNewStandaloneOutcome();
    UUID result = newAddressReportedProcessor.process(outcome, outcome.getCaseId(), "CE");

    verify(gatewayOutcomeProducer).logLegacyOutcomeSuppressed(
        eq("Field.other"), eq("NEW_ADDRESS_REPORTED"), eq(String.valueOf(outcome.getTransactionId())));
    verify(gatewayOutcomeProducer, never()).sendOutcome(any(), any());
    verify(cacheService).save(any(GatewayCaseRecord.class));
    verify(eventManager, never())
      .triggerEvent(eq(outcome.getCaseId().toString()), eq("OUTCOME_SENT"), any(String[].class));
    org.assertj.core.api.Assertions.assertThat(result).isEqualTo(outcome.getCaseId());
  }
}
