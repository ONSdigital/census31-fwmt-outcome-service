package uk.gov.ons.census.fwmt.outcomeservice.converter.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.common.rm.dto.ActionInstructionType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtCancelActionInstruction;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.OutcomePublicationContext;
import uk.gov.ons.census.fwmt.outcomeservice.message.PubSubFieldworkActionInstructionPublisher;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.SwitchCaseIdService;

@ExtendWith(MockitoExtension.class)
class FieldworkActionInstructionProcessorTest {

  @Mock
  private PubSubFieldworkActionInstructionPublisher fieldworkActionInstructionPublisher;

  @Mock
  private GatewayEventManager gatewayEventManager;

  @Mock
  private SwitchCaseIdService switchCaseIdService;

  @InjectMocks
  private CancelFeedbackProcessor cancelFeedbackProcessor;

  @InjectMocks
  private SwitchFeedbackCeSiteProcessor switchFeedbackCeSiteProcessor;

  @InjectMocks
  private SwitchFeedbackEstFProcessor switchFeedbackEstFProcessor;

  @InjectMocks
  private SwitchFeedbackUnitFProcessor switchFeedbackUnitFProcessor;

  @Captor
  private ArgumentCaptor<FwmtCancelActionInstruction> cancelInstructionCaptor;

  @Captor
  private ArgumentCaptor<FwmtActionInstruction> actionInstructionCaptor;

  @Captor
  private ArgumentCaptor<OutcomePublicationContext> publicationContextCaptor;

  @Test
  void publishesNcFeedbackCancelUsingTheLoggedCaseId() throws Exception {
    OutcomeSuperSetDto outcome = outcome();
    String loggedCaseId = UUID.randomUUID().toString();
    when(switchCaseIdService.fromIdOriginalToNc(outcome.getCaseId().toString()))
        .thenReturn(loggedCaseId);

    cancelFeedbackProcessor.process(outcome, outcome.getCaseId(), "NC");

    verify(fieldworkActionInstructionPublisher).publish(cancelInstructionCaptor.capture(),
        publicationContextCaptor.capture());
    FwmtCancelActionInstruction instruction = cancelInstructionCaptor.getValue();
    assertThat(instruction.getActionInstruction()).isEqualTo(ActionInstructionType.CANCEL);
    assertThat(instruction.getCaseId()).isEqualTo(loggedCaseId);
    assertThat(publicationContextCaptor.getValue().caseId()).isEqualTo(loggedCaseId);
  }

  @Test
  void publishesCeSiteSwitchUsingTheSiteCaseId() throws Exception {
    OutcomeSuperSetDto outcome = outcome();
    UUID siteCaseId = UUID.randomUUID();
    outcome.setSiteCaseId(siteCaseId);

    switchFeedbackCeSiteProcessor.process(outcome, outcome.getCaseId(), "CE");

    assertSwitchPublication(siteCaseId.toString());
  }

  @Test
  void publishesCeEstFeedbackSwitchUsingTheOutcomeCaseId() throws Exception {
    OutcomeSuperSetDto outcome = outcome();

    switchFeedbackEstFProcessor.process(outcome, outcome.getCaseId(), "CE");

    assertSwitchPublication(outcome.getCaseId().toString());
  }

  @Test
  void publishesCeUnitFeedbackSwitchUsingTheOutcomeCaseId() throws Exception {
    OutcomeSuperSetDto outcome = outcome();

    switchFeedbackUnitFProcessor.process(outcome, outcome.getCaseId(), "CE");

    assertSwitchPublication(outcome.getCaseId().toString());
  }

  private void assertSwitchPublication(String expectedCaseId) {
    verify(fieldworkActionInstructionPublisher).publish(actionInstructionCaptor.capture(),
        publicationContextCaptor.capture());
    FwmtActionInstruction instruction = actionInstructionCaptor.getValue();
    assertThat(instruction.getActionInstruction()).isEqualTo(ActionInstructionType.SWITCH_CE_TYPE);
    assertThat(instruction.getCaseId()).isEqualTo(expectedCaseId);
    assertThat(publicationContextCaptor.getValue().caseId()).isEqualTo(expectedCaseId);
  }

  private OutcomeSuperSetDto outcome() {
    OutcomeSuperSetDto outcome = new OutcomeSuperSetDto();
    outcome.setCaseId(UUID.randomUUID());
    outcome.setTransactionId(UUID.randomUUID());
    outcome.setEventDate(Date.from(Instant.parse("2026-09-22T10:15:30Z")));
    return outcome;
  }
}