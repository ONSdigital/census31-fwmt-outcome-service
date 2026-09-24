package uk.gov.ons.census.fwmt.outcomeservice.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeLookup;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class OutcomeServiceImplTest {

  @InjectMocks
  private OutcomeServiceImpl outcomeService;

  @Mock
  private OutcomeLookup outcomeLookup;

  @Mock
  private OutcomeServiceProcessor fulfilmentProcessor;

  @Mock
  private GatewayEventManager gatewayEventManager;

  @Test
  void skipsUnsupportedOperationAndContinuesLaterOperations(CapturedOutput output) throws Exception {
    OutcomeSuperSetDto outcome = new OutcomeSuperSetDto();
    outcome.setCaseId(UUID.fromString("123e4567-e89b-12d3-a456-426614174111"));
    outcome.setOutcomeCode("20-20-01");
    outcome.setTransactionId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    when(outcomeLookup.getLookup("20-20-01"))
        .thenReturn(new String[] {"LINKED_QID", "FULFILMENT_REQUESTED"});
    when(fulfilmentProcessor.process(eq(outcome), eq(null), eq("CE"))).thenReturn(outcome.getCaseId());
    ReflectionTestUtils.setField(outcomeService, "outcomeServiceProcessors",
        Map.of("FULFILMENT_REQUESTED", fulfilmentProcessor));

    assertThatCode(() -> outcomeService.createCeOutcomeEvent(outcome)).doesNotThrowAnyException();

    verify(fulfilmentProcessor).process(eq(outcome), eq(null), eq("CE"));
    org.assertj.core.api.Assertions.assertThat(output.getOut())
        .contains("Legacy queue Field.other is retired; operation LINKED_QID has no replacement topic; event not published");
  }
}