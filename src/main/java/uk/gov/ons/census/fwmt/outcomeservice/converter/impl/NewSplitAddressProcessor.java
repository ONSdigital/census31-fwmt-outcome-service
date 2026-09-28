package uk.gov.ons.census.fwmt.outcomeservice.converter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCaseRecordService;
import java.util.UUID;

import static uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceLogConfig.*;
import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.NEW_ADDRESS_REPORTED;
import static uk.gov.ons.census.fwmt.outcomeservice.util.SpgUtilityMethods.isDelivered;

@Component("NEW_SPLIT_ADDRESS")
public class NewSplitAddressProcessor implements OutcomeServiceProcessor {

  @Autowired
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private GatewayCaseRecordService gatewayCacheService;

  @Override
  public UUID process(OutcomeSuperSetDto outcome, UUID caseIdHolder, String type) throws GatewayException {
    UUID caseId = (caseIdHolder != null) ? caseIdHolder : outcome.getCaseId();

    gatewayEventManager.triggerEvent(String.valueOf(caseId), PROCESSING_OUTCOME,
        SURVEY_TYPE, type,
        PROCESSOR, "NEW_SPLIT_ADDRESS",
        ORIGINAL_CASE_ID, String.valueOf(outcome.getCaseId()),
        SITE_CASE_ID, (outcome.getSiteCaseId() != null ? String.valueOf(outcome.getSiteCaseId()) : "N/A"));

    boolean isDelivered = isDelivered(outcome);
    cacheData(outcome, outcome.getCaseId(), isDelivered);

    // Historical NEW_ADDRESS_REPORTED template values (not current runtime behavior):
    // sourceCase: "NEW_SPLIT_ADDRESS".
    // outcome: the outcome DTO.
    // newCaseId: selected case ID.
    // surveyType: processor survey-type argument.
    // addressLevel: "U".
    // sourceCaseId: outcome.getOriginatingCaseId().
    // region: value derived by regionLookup(outcome.getOfficerId()).
    // officerId: outcome officer ID.
    // address: outcome address.
    // eventDate: formatted outcome event date.
    // This legacy message is intentionally suppressed; do not rebuild or publish it without an
    // approved replacement contract.
    gatewayOutcomeProducer.logLegacyOutcomeSuppressed(
      "Field.other", NEW_ADDRESS_REPORTED.toString(), String.valueOf(outcome.getTransactionId()));

    return caseId;
  }

  private void cacheData(OutcomeSuperSetDto outcome, UUID caseId, boolean isDelivered) throws GatewayException {
    GatewayCaseRecord cache = gatewayCacheService.getById(String.valueOf(caseId));
    if (cache != null) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, "Case already exists in cache: {}", caseId);
    }

    gatewayCacheService.save(GatewayCaseRecord.builder()
        .caseId(String.valueOf(caseId))
        .delivered(isDelivered)
        .existsInFwmt(false)
        .accessInfo(outcome.getAccessInfo())
        .careCodes(OutcomeSuperSetDto.careCodesToText(outcome.getCareCodes()))
        .type(10)
        .build());
  }
}
