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
import uk.gov.ons.census.fwmt.common.data.ccs.CCSInterviewOutcome;
import uk.gov.ons.census.fwmt.common.data.ccs.CCSPropertyListingOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingPublisher;
import uk.gov.ons.census.fwmt.outcomeservice.openapi.SurveyFlaggedEndpoint;
import uk.gov.ons.census.fwmt.outcomeservice.service.OutcomeFeatureFlagGuard;

import java.util.UUID;

@RestController
@SurveyFlaggedEndpoint("CCS")
@Tag(name = "CCS Outcomes", description = "CCS survey outcome endpoints")
public class CcsOutcomeController {

  private static final String COMET_CCS_PL_RECEIVED = "COMET_CCS_PL_RECEIVED";
  private static final String COMET_CCS_INT_RECEIVED = "COMET_CCS_INT_RECEIVED";

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private OutcomePreprocessingPublisher outcomePreprocessingProducer;

  @Autowired
  private OutcomeFeatureFlagGuard featureFlagGuard;

  @Operation(summary = "Post a CCS property listing outcome to the FWMT Gateway")
  @PostMapping(value = "/ccsPropertyListingOutcome", produces = {"application/json"})
  public ResponseEntity<Void> ccsPropertyListing(@RequestBody CCSPropertyListingOutcome ccsPropertyListingOutcome) throws GatewayException {
    if (!featureFlagGuard.isEnabled("CCS")) {
      return featureFlagGuard.handleDisabledSurvey("CCS", "/ccsPropertyListingOutcome");
    }
    gatewayEventManager.triggerEvent(String.valueOf(ccsPropertyListingOutcome.getCaseId()), COMET_CCS_PL_RECEIVED,
        "transactionId", ccsPropertyListingOutcome.getTransactionId().toString(),
        "Survey type", "CCS PL",
        "Primary Outcome", ccsPropertyListingOutcome.getPrimaryOutcomeDescription(),
        "Secondary Outcome", ccsPropertyListingOutcome.getSecondaryOutcomeDescription(),
        "Outcome code", ccsPropertyListingOutcome.getOutcomeCode(),
        "CCSPropertyListing", ccsPropertyListingOutcome.toString());
    outcomePreprocessingProducer.sendCcsPropertyListingToPreprocessingQueue(ccsPropertyListingOutcome);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Operation(summary = "Post a CCS Interview outcome to the FWMT Gateway")
  @PostMapping(value = "/ccsInterviewOutcome/{caseID}", produces = {"application/json"})
  public ResponseEntity<Void> ccsInterview(
      @PathVariable("caseID") String caseID, @RequestBody CCSInterviewOutcome ccsInterviewOutcome) throws GatewayException {
    if (!featureFlagGuard.isEnabled("CCS")) {
      return featureFlagGuard.handleDisabledSurvey("CCS", "/ccsInterviewOutcome/{caseID}");
    }
    gatewayEventManager.triggerEvent(caseID, COMET_CCS_INT_RECEIVED,
        "transactionId", ccsInterviewOutcome.getTransactionId().toString(),
        "Survey type", "CCS INT",
        "Primary Outcome", ccsInterviewOutcome.getPrimaryOutcomeDescription(),
        "Secondary Outcome", ccsInterviewOutcome.getSecondaryOutcomeDescription(),
        "Outcome code", ccsInterviewOutcome.getOutcomeCode(),
        "CCSInterview", ccsInterviewOutcome.toString());
    ccsInterviewOutcome.setCaseId(UUID.fromString(caseID));
    outcomePreprocessingProducer.sendCcsInterviewToPreprocessingQueue(ccsInterviewOutcome);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}

