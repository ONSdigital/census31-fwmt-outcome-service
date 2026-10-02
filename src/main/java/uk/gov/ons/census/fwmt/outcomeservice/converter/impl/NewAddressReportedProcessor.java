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

@Component("NEW_ADDRESS_REPORTED")
public class NewAddressReportedProcessor implements OutcomeServiceProcessor {

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
        PROCESSOR, "NEW_ADDRESS_REPORTED",
        ORIGINAL_CASE_ID, String.valueOf(outcome.getCaseId()));

    boolean isDelivered = isDelivered(outcome);
    cacheData(outcome, caseId, isDelivered);

    // Historical NEW_ADDRESS_REPORTED template values (not current runtime behavior):
    // sourceCase: "NEW_STANDALONE".
    // outcome: the outcome DTO.
    // ceDetails: present CE details; establishmentType and establishmentSecure defaulted to
    //     "OTHER" and "false" when absent.
    // usualResidents: CE usual-resident count when details were present, defaulting to 0.
    // newCaseId: selected case ID.
    // address: outcome address.
    // officerId: outcome officer ID.
    // eventDate: formatted outcome event date.
    // surveyType: processor survey-type argument.
    // region: value derived by regionLookup(outcome.getOfficerId()).
    // addressLevel: "E" for CE; "U" for HH or SPG; other types were rejected.
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
        .build());
  }
}
