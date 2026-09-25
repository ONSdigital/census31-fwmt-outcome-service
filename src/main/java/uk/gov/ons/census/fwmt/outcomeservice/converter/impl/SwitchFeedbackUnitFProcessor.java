package uk.gov.ons.census.fwmt.outcomeservice.converter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.rm.dto.ActionInstructionType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.OutcomePublicationContext;
import uk.gov.ons.census.fwmt.outcomeservice.message.PubSubFieldworkActionInstructionPublisher;

import java.util.UUID;

import static uk.gov.ons.census.fwmt.common.data.tm.SurveyType.CE_UNIT_F;
import static uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceLogConfig.*;

@Component("SWITCH_FEEDBACK_CE_UNIT_F")
public class SwitchFeedbackUnitFProcessor implements OutcomeServiceProcessor {

  @Autowired
  private PubSubFieldworkActionInstructionPublisher fieldworkActionInstructionPublisher;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Override
  public UUID process(OutcomeSuperSetDto outcome, UUID caseIdHolder, String type) throws GatewayException {
    UUID caseId = (caseIdHolder != null) ? caseIdHolder : outcome.getCaseId();

    gatewayEventManager.triggerEvent(String.valueOf(caseId), PROCESSING_OUTCOME,
    SURVEY_TYPE, type,
    PROCESSOR, "SWITCH_FEEDBACK_CE_UNIT_F",
    ORIGINAL_CASE_ID, String.valueOf(outcome.getCaseId()),
    SITE_CASE_ID, (outcome.getSiteCaseId() != null ? String.valueOf(outcome.getSiteCaseId()) : "N/A"));

    FwmtActionInstruction fieldworkFollowup = FwmtActionInstruction.builder()
        .actionInstruction(ActionInstructionType.SWITCH_CE_TYPE)
        .surveyName("CENSUS")
        .addressType(type)
        .surveyType(CE_UNIT_F)
        .caseId(caseId.toString())
        .build();

    fieldworkActionInstructionPublisher.publish(fieldworkFollowup,
      new OutcomePublicationContext(fieldworkFollowup.getCaseId(), outcome.getTransactionId(),
        outcome.getEventDate(), ""));

    gatewayEventManager.triggerEvent(String.valueOf(caseId), FIELDWORK_ACTION_INSTRUCTION_PUBLISH,
        SURVEY_NAME, "CENSUS",
        ADDRESS_TYPE, type,
        SWITCH_TYPE, CE_UNIT_F.toString(),
    ACTION_INSTRUCTION_TYPE, ActionInstructionType.SWITCH_CE_TYPE.toString(),
    TRANSACTION_ID, outcome.getTransactionId().toString());

    return caseId;
  }
}
