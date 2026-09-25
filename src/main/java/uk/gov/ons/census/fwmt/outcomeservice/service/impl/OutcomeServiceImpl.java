package uk.gov.ons.census.fwmt.outcomeservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeLookup;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.OutcomeOperationContext;
import uk.gov.ons.census.fwmt.outcomeservice.service.OutcomeService;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class OutcomeServiceImpl implements OutcomeService {

  public static final String PROCESSING_HH_OUTCOME = "PROCESSING_HH_OUTCOME";

  public static final String PROCESSING_SPG_OUTCOME = "PROCESSING_SPG_OUTCOME";

  public static final String PROCESSING_CE_OUTCOME = "PROCESSING_CE_OUTCOME";

  public static final String PROCESSING_CCS_PL_OUTCOME = "PROCESSING_CCS_PL_OUTCOME";

  public static final String PROCESSING_CCS_INT_OUTCOME = "PROCESSING_CCS_INT_OUTCOME";

  public static final String PROCESSING_NC_OUTCOME = "PROCESSING_NC_OUTCOME";

  public static final String FAILED_TO_LOOKUP_OUTCOME_CODE = "FAILED_TO_LOOKUP_OUTCOME_CODE";

  @Autowired
  private Map<String, OutcomeServiceProcessor> outcomeServiceProcessors;

  @Autowired
  private OutcomeLookup outcomeLookup;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Override
  @Transactional
  public void createSpgOutcomeEvent(OutcomeSuperSetDto outcome) throws GatewayException {
    processOutcome(outcome, "SPG", PROCESSING_SPG_OUTCOME, "Failed to  process SpgOutcome");
  }

  @Override
  @Transactional
  public void createCeOutcomeEvent(OutcomeSuperSetDto outcome) throws GatewayException {
    processOutcome(outcome, "CE", PROCESSING_CE_OUTCOME, "Failed to  process CeOutcome");
  }

  @Override
  @Transactional
  public void createHhOutcomeEvent(OutcomeSuperSetDto outcome) throws GatewayException {
    processOutcome(outcome, "HH", PROCESSING_HH_OUTCOME, "Failed to  process HhOutcome");
  }

  @Override
  @Transactional
  public void createCcsPropertyListingOutcomeEvent(OutcomeSuperSetDto outcome) throws GatewayException {
    processOutcome(outcome, "CCS PL", PROCESSING_CCS_PL_OUTCOME, "Failed to  process CcsPlOutcome");
  }

  @Override
  @Transactional
  public void createCcsInterviewOutcomeEvent(OutcomeSuperSetDto outcome) throws GatewayException {
    processOutcome(outcome, "CCS INT", PROCESSING_CCS_INT_OUTCOME, "Failed to  process CcsIntOutcome");
  }

  @Override
  @Transactional
  public void createNcOutcomeEvent(OutcomeSuperSetDto outcome) throws GatewayException {
    processOutcome(outcome, "NC", PROCESSING_NC_OUTCOME, "Failed to  process NcOutcome");
  }

  private void processOutcome(
      OutcomeSuperSetDto outcome,
      String surveyType,
      String processingEvent,
      String failureMessage)
      throws GatewayException {
    String[] operationsList = outcomeLookup.getLookup(outcome.getOutcomeCode());
    if (operationsList == null) {
      gatewayEventManager.triggerErrorEvent(this.getClass(), (Exception) null, "No outcome code found",
          String.valueOf(outcome.getCaseId()), FAILED_TO_LOOKUP_OUTCOME_CODE,
          "Survey type", surveyType,
          "Outcome code", outcome.getOutcomeCode(),
          "Secondary Outcome", outcome.getSecondaryOutcomeDescription());
      throw new GatewayException(GatewayException.Fault.BAD_REQUEST, failureMessage);
    }

    UUID caseIdHolder = null;
    for (String operation : operationsList) {
      gatewayEventManager.triggerEvent(String.valueOf(outcome.getCaseId()), processingEvent,
          "Survey type", surveyType,
          "Secondary Outcome", outcome.getSecondaryOutcomeDescription(),
          "Held case id", (caseIdHolder != null) ? String.valueOf(caseIdHolder) : "N/A",
          "Operation", operation,
          "Operation list", Arrays.toString(operationsList));

      OutcomeServiceProcessor processor = outcomeServiceProcessors.get(operation);
      if (processor == null) {
        log.error(
            "Legacy queue {} is retired; operation {} has no replacement topic; event not published transactionId={} outcomeCode={} caseId={} surveyType={}",
            GatewayOutcomeQueueConfig.retiredQueueForOperation(operation),
            operation,
            outcome.getTransactionId(),
            outcome.getOutcomeCode(),
            (caseIdHolder != null) ? caseIdHolder : outcome.getCaseId(),
            surveyType);
        continue;
      }

      OutcomeOperationContext.set(new OutcomeOperationContext.Details(
          operation,
          outcome.getOutcomeCode(),
          outcome.getTransactionId() != null ? outcome.getTransactionId().toString() : "UNKNOWN",
          String.valueOf((caseIdHolder != null) ? caseIdHolder : outcome.getCaseId()),
          surveyType));
      try {
        caseIdHolder = processor.process(outcome, caseIdHolder, surveyType);
      } finally {
        OutcomeOperationContext.clear();
      }
    }
  }
}
