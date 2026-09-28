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
import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.CCS_ADDRESS_LISTED;

@Component("PROPERTY_LISTED_HH")
public class PropertyListedHhProcessor implements OutcomeServiceProcessor {

  @Autowired
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private GatewayCaseRecordService gatewayCacheService;

  @Override
  public UUID process(OutcomeSuperSetDto outcome, UUID caseIdHolder, String type) throws GatewayException {
    UUID caseId = (caseIdHolder != null) ? caseIdHolder : outcome.getCaseId();
    UUID newCaseId = UUID.randomUUID();

    gatewayEventManager.triggerEvent(String.valueOf(caseId), PROCESSING_OUTCOME,
        SURVEY_TYPE, type,
        PROCESSOR, "PROPERTY_LISTED_HH",
        ORIGINAL_CASE_ID, String.valueOf(outcome.getCaseId()),
        PROPERTY_LISTED_CASE_ID, String.valueOf(newCaseId),
        ADDRESS_TYPE, "HH");

    cacheData(outcome, newCaseId);
    // Historical CCS_ADDRESS_LISTED template values (not current runtime behavior):
    // outcome: the outcome DTO.
    // address: outcome address.
    // caseId: generated property-listed case ID.
    // eventDate: formatted outcome event date.
    // addressType: "HH".
    // addressLevel: "U".
    // interviewRequired: "False".
    // oa: value from the parent/original case cache looked up by the selected caseId.
    // region: first character of the cached OA.
    // This legacy message is intentionally suppressed; do not rebuild or publish it without an
    // approved replacement contract.
    gatewayOutcomeProducer.logLegacyOutcomeSuppressed(
      "Field.other", CCS_ADDRESS_LISTED.toString(), String.valueOf(outcome.getTransactionId()));

    return newCaseId;
  }

  private void cacheData(OutcomeSuperSetDto outcome, UUID newCaseId) {
    gatewayCacheService.save(GatewayCaseRecord.builder()
        .caseId(newCaseId.toString())
        .existsInFwmt(false)
        .accessInfo(outcome.getAccessInfo())
        .careCodes(OutcomeSuperSetDto.careCodesToText(outcome.getCareCodes()))
        .type(50)
        .build());
  }
}
