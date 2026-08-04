package uk.gov.ons.census.fwmt.outcomeservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.fwmt.common.data.nc.NCOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingPublisher;
import uk.gov.ons.census.fwmt.outcomeservice.openapi.SurveyFlaggedEndpoint;
import uk.gov.ons.census.fwmt.outcomeservice.service.OutcomeFeatureFlagGuard;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.SwitchCaseIdService;

import java.util.UUID;

@RestController
@SurveyFlaggedEndpoint("NC")
@Tag(name = "NC Outcomes", description = "NC (Non-Compliance) survey outcome endpoints")
public class NcOutcomeController {

  private static final String COMET_NC_OUTCOME_RECEIVED = "COMET_NC_OUTCOME_RECEIVED";

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private OutcomePreprocessingPublisher outcomePreprocessingProducer;

  @Autowired
  private SwitchCaseIdService switchCaseIdService;

  @Autowired
  private OutcomeFeatureFlagGuard featureFlagGuard;

  @Operation(summary = "Post a Non-Compliance outcome to the FWMT Gateway")
  @PostMapping(value = "/ncOutcome/{caseID}", produces = {"application/json"})
  public ResponseEntity<Void> ncOutcome(
      @PathVariable("caseID") String caseID, @RequestBody NCOutcome ncOutcome) throws GatewayException {
    if (!featureFlagGuard.isEnabled("NC")) {
      return featureFlagGuard.handleDisabledSurvey("NC", "/ncOutcome/{caseID}");
    }
    String hhCaseId = switchCaseIdService.fromNcToOriginal(caseID);
    ncOutcome.setCaseId(UUID.fromString(hhCaseId));
    gatewayEventManager.triggerEvent(caseID, COMET_NC_OUTCOME_RECEIVED,
        "transactionId", ncOutcome.getTransactionId().toString(),
        "Original HH CaseId", hhCaseId,
        "Survey type", "NC",
        "Primary Outcome", ncOutcome.getPrimaryOutcomeDescription(),
        "Secondary Outcome", ncOutcome.getSecondaryOutcomeDescription(),
        "Outcome code", ncOutcome.getOutcomeCode(),
        "NCOutcome", ncOutcome.toString());
    outcomePreprocessingProducer.sendHHStandaloneAddressToPreprocessingQueue(ncOutcome);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}

