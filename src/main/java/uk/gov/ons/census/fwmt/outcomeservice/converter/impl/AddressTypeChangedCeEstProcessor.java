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
import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.ADDRESS_TYPE_CHANGED;

@Component("ADDRESS_TYPE_CHANGED_CE_EST")
public class AddressTypeChangedCeEstProcessor implements OutcomeServiceProcessor {

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
        PROCESSOR, "ADDRESS_TYPE_CHANGED_CE_EST",
        ORIGINAL_CASE_ID, String.valueOf(outcome.getCaseId()),
        SITE_CASE_ID, (outcome.getSiteCaseId() != null ? String.valueOf(outcome.getSiteCaseId()) : "N/A"));

    UUID newCaseId = UUID.randomUUID();
    cacheData(outcome, caseId, newCaseId);
    gatewayOutcomeProducer.logLegacyOutcomeSuppressed(
      "Field.other", ADDRESS_TYPE_CHANGED.toString(), String.valueOf(outcome.getTransactionId()));

    return newCaseId;
  }

  private void cacheData(OutcomeSuperSetDto outcome, UUID caseId, UUID newCaseId) throws GatewayException {
    GatewayCaseRecord parentCacheJob = gatewayCacheService.getById(caseId.toString());
    if (parentCacheJob == null) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, "Parent case does not exist in cache: {}", caseId);
    }

    GatewayCaseRecord newCachedJob = gatewayCacheService.getById(newCaseId.toString());
    if (newCachedJob != null) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, "New case exists in cache: {}", caseId);
    }

    gatewayCacheService.save(GatewayCaseRecord.builder()
        .caseId(newCaseId.toString())
        .existsInFwmt(false)
        .accessInfo(outcome.getAccessInfo())
        .careCodes(OutcomeSuperSetDto.careCodesToText(outcome.getCareCodes()))
        .build());
  }
}

