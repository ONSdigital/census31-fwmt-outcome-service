package uk.gov.ons.census.fwmt.outcomeservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.fwmt.common.data.household.HHNewSplitAddress;
import uk.gov.ons.census.fwmt.common.data.household.HHNewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.household.HHOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingPublisher;

@RestController
@ConditionalOnProperty(name = "feature-flags.outcome.surveys.HH", havingValue = "true", matchIfMissing = false)
@Tag(name = "HH Outcomes", description = "HH survey outcome endpoints")
public class HhOutcomeController {

  private static final String COMET_HH_OUTCOME_RECEIVED = "COMET_HH_OUTCOME_RECEIVED";
  private static final String COMET_HH_SPLITADDRESS_RECEIVED = "COMET_HH_SPLITADDRESS_RECEIVED";
  private static final String COMET_HH_STANDALONE_RECEIVED = "COMET_HH_STANDALONE_RECEIVED";

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private OutcomePreprocessingPublisher outcomePreprocessingProducer;

  @Operation(summary = "Post a HH survey outcome to the FWMT Gateway")
  @PostMapping(value = "/hhOutcome/{caseID}", produces = {"application/json"})
  public ResponseEntity<Void> hhOutcomeResponse(
      @PathVariable("caseID") String caseID, @RequestBody HHOutcome hhOutcome) throws GatewayException {
    gatewayEventManager.triggerEvent(caseID, COMET_HH_OUTCOME_RECEIVED,
        "transactionId", hhOutcome.getTransactionId().toString(),
        "Survey type", "HH",
        "Primary Outcome", hhOutcome.getPrimaryOutcomeDescription(),
        "Secondary Outcome", hhOutcome.getSecondaryOutcomeDescription(),
        "Outcome code", hhOutcome.getOutcomeCode(),
        "HHOutcome", hhOutcome.toString());
    outcomePreprocessingProducer.sendHHOutcomeToPreprocessingQueue(hhOutcome);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Operation(summary = "Post a HH survey new split address outcome to the FWMT Gateway")
  @PostMapping(value = "/hhOutcome/splitAddress/new", produces = {"application/json"})
  public ResponseEntity<Void> hhNewSplitAddress(@RequestBody HHNewSplitAddress hhNewSplitAddress) throws GatewayException {
    gatewayEventManager.triggerEvent("N/A", COMET_HH_SPLITADDRESS_RECEIVED,
        "transactionId", hhNewSplitAddress.getTransactionId().toString(),
        "Survey type", "HH",
        "Primary Outcome", hhNewSplitAddress.getPrimaryOutcomeDescription(),
        "Secondary Outcome", hhNewSplitAddress.getSecondaryOutcomeDescription(),
        "Outcome code", hhNewSplitAddress.getOutcomeCode(),
        "HHNewSplitAddress", hhNewSplitAddress.toString());
    outcomePreprocessingProducer.sendHHSplitAddressToPreprocessingQueue(hhNewSplitAddress);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Operation(summary = "Post a HH survey new standalone address outcome to the FWMT Gateway")
  @PostMapping(value = "/hhOutcome/standaloneAddress/new", produces = {"application/json"})
  public ResponseEntity<Void> hhNewStandalone(@RequestBody HHNewStandaloneAddress hhNewStandaloneAddress) throws GatewayException {
    gatewayEventManager.triggerEvent("N/A", COMET_HH_STANDALONE_RECEIVED,
        "transactionId", hhNewStandaloneAddress.getTransactionId().toString(),
        "Survey type", "HH",
        "Primary Outcome", hhNewStandaloneAddress.getPrimaryOutcomeDescription(),
        "Secondary Outcome", hhNewStandaloneAddress.getSecondaryOutcomeDescription(),
        "Outcome code", hhNewStandaloneAddress.getOutcomeCode(),
        "HHNewStandaloneAddress", hhNewStandaloneAddress.toString());
    outcomePreprocessingProducer.sendHHStandaloneAddressToPreprocessingQueue(hhNewStandaloneAddress);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}

