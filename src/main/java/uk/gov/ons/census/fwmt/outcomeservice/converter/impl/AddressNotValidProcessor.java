package uk.gov.ons.census.fwmt.outcomeservice.converter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.ReasonCodeLookup;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.EventDictionaryMessageFactory;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;

import java.util.UUID;

import static uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceLogConfig.*;
import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.ADDRESS_NOT_VALID;

@Component("ADDRESS_NOT_VALID")
public class AddressNotValidProcessor implements OutcomeServiceProcessor {

  @Autowired
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Autowired
  private EventDictionaryMessageFactory eventDictionaryMessageFactory;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private ReasonCodeLookup reasonCodeLookup;

  @Override
  public UUID process(OutcomeSuperSetDto outcome, UUID caseIdHolder, String type) throws GatewayException {
    UUID caseId = (caseIdHolder != null) ? caseIdHolder : outcome.getCaseId();

    gatewayEventManager.triggerEvent(String.valueOf(caseId), PROCESSING_OUTCOME,
        SURVEY_TYPE, type,
        PROCESSOR, "ADDRESS_NOT_VALID",
        ORIGINAL_CASE_ID, String.valueOf(outcome.getCaseId()),
        SITE_CASE_ID, (outcome.getSiteCaseId() != null ? String.valueOf(outcome.getSiteCaseId()) : "N/A"));

    String reasonCode = reasonCodeLookup.getLookup(outcome.getOutcomeCode());

    if (reasonCode.equals("NOT_FOUND")) {
      gatewayEventManager.triggerErrorEvent(this.getClass(), "No reason code found",
          String.valueOf(outcome.getCaseId()), FAILED_TO_LOOKUP_REASON_CODE,
          SURVEY_TYPE, type,
          OUTCOME_CODE, outcome.getOutcomeCode(),
          SECONDARY_OUTCOME, outcome.getSecondaryOutcomeDescription());
    }

    String outcomeEvent = eventDictionaryMessageFactory.buildAddressNotValid(reasonCode, caseId.toString());

    gatewayOutcomeProducer.sendOutcome(outcomeEvent, String.valueOf(outcome.getTransactionId()),
      GatewayOutcomeQueueConfig.GATEWAY_ADDRESS_NOT_VALID_ROUTING_KEY);

    gatewayEventManager.triggerEvent(String.valueOf(caseId), OUTCOME_SENT,
        SURVEY_TYPE, type,
        TEMPLATE_TYPE, ADDRESS_NOT_VALID.toString(),
        TRANSACTION_ID, outcome.getTransactionId().toString(),
        ROUTING_KEY, GatewayOutcomeQueueConfig.GATEWAY_ADDRESS_NOT_VALID_ROUTING_KEY);
    return caseId;
  }
}
