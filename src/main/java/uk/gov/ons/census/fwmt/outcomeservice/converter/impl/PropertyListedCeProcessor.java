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

@Component("PROPERTY_LISTED_CE")
public class PropertyListedCeProcessor implements OutcomeServiceProcessor {

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

    // PROCESSING_OUTCOME telemetry remains intentional after retiring the legacy RM message;
    // it records processing for the original case and generated property-listed case. Do not
    // emit OUTCOME_SENT: no outcome message is published by this processor.
    gatewayEventManager.triggerEvent(String.valueOf(caseId), PROCESSING_OUTCOME,
        SURVEY_TYPE, type,
        PROCESSOR, "PROPERTY_LISTED_CE",
        ORIGINAL_CASE_ID, String.valueOf(outcome.getCaseId()),
        PROPERTY_LISTED_CASE_ID, String.valueOf(newCaseId),
        ADDRESS_TYPE, "CE");

    cacheData(outcome, newCaseId);

    // Historical CCS_ADDRESS_LISTED template values (not current runtime behavior):
    // outcome: the outcome DTO.
    // address: outcome address.
    // caseId: newCaseId, the generated property-listed case ID.
    // eventDate: formatted outcome event date.
    // addressType: "CE".
    // addressLevel: "E".
    // interviewRequired: "False".
    // oa: value from the parent/original case cache, loaded by
    //     gatewayCacheService.getById(String.valueOf(caseId)).
    // region: first character of the cached OA.
    // estabType: establishment type, defaulting to "CE".
    // organisationName: establishment name, defaulting to an empty string.
    // There is no approved Event Dictionary destination for this legacy message, so it is
    // intentionally suppressed; do not rebuild or publish it without an approved replacement
    // contract.
    gatewayOutcomeProducer.logLegacyOutcomeSuppressed(
      "Field.other", CCS_ADDRESS_LISTED.toString(), String.valueOf(outcome.getTransactionId()));

    return newCaseId;
  }
  private void cacheData(OutcomeSuperSetDto outcome, UUID newCaseId) {
    String managerTitle = "";
    String managerForename = "";
    String managerSurname = "";
    String managerPhone = "";
    int usualResidents = 0;
    int bedspaces = 0;

    if (outcome.getCeDetails() != null) {
      if (outcome.getCeDetails().getManagerTitle() != null) {
        managerTitle = outcome.getCeDetails().getManagerTitle();
      }
      if (outcome.getCeDetails().getManagerForename() != null) {
        managerForename = outcome.getCeDetails().getManagerForename();
      }
      if (outcome.getCeDetails().getManagerSurname() != null) {
        managerSurname = outcome.getCeDetails().getManagerSurname();
      }
      if (outcome.getCeDetails().getContactPhone() != null) {
        managerPhone = outcome.getCeDetails().getContactPhone();
      }
      if (outcome.getCeDetails().getUsualResidents() != null) {
        usualResidents = outcome.getCeDetails().getUsualResidents();
      }
      if (outcome.getCeDetails().getBedspaces() != null) {
        bedspaces = outcome.getCeDetails().getBedspaces();
      }
    }

    gatewayCacheService.save(GatewayCaseRecord.builder()
        .caseId(newCaseId.toString())
        .existsInFwmt(false)
        .accessInfo(outcome.getAccessInfo())
        .careCodes(OutcomeSuperSetDto.careCodesToText(outcome.getCareCodes()))
        .type(50)
        .managerTitle(managerTitle)
        .managerFirstname(managerForename)
        .managerSurname(managerSurname)
        .managerContactNumber(managerPhone)
        .usualResidents(usualResidents)
        .bedspaces(bedspaces)
        .build());
  }
}
