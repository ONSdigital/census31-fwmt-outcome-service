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
import uk.gov.ons.census.fwmt.common.data.spg.SPGNewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.spg.SPGNewUnitAddress;
import uk.gov.ons.census.fwmt.common.data.spg.SPGOutcome;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingPublisher;

import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "feature-flags.outcome.surveys.SPG", havingValue = "true", matchIfMissing = false)
@Tag(name = "SPG Outcomes", description = "SPG survey outcome endpoints")
public class SpgOutcomeController {

  private static final String COMET_SPG_OUTCOME_RECEIVED = "COMET_SPG_OUTCOME_RECEIVED";
  private static final String COMET_SPG_UNITADDRESS_OUTCOME_RECEIVED = "COMET_SPG_UNITADDRESS_OUTCOME_RECEIVED";
  private static final String COMET_SPG_STANDALONE_OUTCOME_RECEIVED = "COMET_SPG_STANDALONE_OUTCOME_RECEIVED";

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private OutcomePreprocessingPublisher outcomePreprocessingProducer;

  @Operation(summary = "Post a SPG survey outcome to the FWMT Gateway")
  @PostMapping(value = "/spgOutcome/{caseID}", produces = {"application/json"})
  public ResponseEntity<Void> spgOutcomeResponse(
      @PathVariable("caseID") String caseId, @RequestBody SPGOutcome spgOutcome) {
    gatewayEventManager.triggerEvent(caseId, COMET_SPG_OUTCOME_RECEIVED,
        "transactionId", spgOutcome.getTransactionId().toString(),
        "Survey type", "SPG",
        "Primary Outcome", spgOutcome.getPrimaryOutcomeDescription(),
        "Secondary Outcome", spgOutcome.getSecondaryOutcomeDescription(),
        "Outcome code", spgOutcome.getOutcomeCode(),
        "SPGOutcome", spgOutcome.toString());
    spgOutcome.setCaseId(UUID.fromString(caseId));
    outcomePreprocessingProducer.sendSpgOutcomeToPreprocessingQueue(spgOutcome);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Operation(summary = "Post a SPG survey new unit address outcome to the FWMT Gateway")
  @PostMapping(value = "/spgOutcome/unitAddress/new", produces = {"application/json"})
  public ResponseEntity<Void> spgNewUnitAddress(@RequestBody SPGNewUnitAddress newUnitAddress) {
    gatewayEventManager.triggerEvent("N/A", COMET_SPG_UNITADDRESS_OUTCOME_RECEIVED,
        "transactionId", newUnitAddress.getTransactionId().toString(),
        "Survey type", "SPG",
        "Primary Outcome", newUnitAddress.getPrimaryOutcomeDescription(),
        "Secondary Outcome", newUnitAddress.getSecondaryOutcomeDescription(),
        "Outcome code", newUnitAddress.getOutcomeCode(),
        "SPGNewUnitAddress", newUnitAddress.toString());
    outcomePreprocessingProducer.sendSpgNewUnitAddressToPreprocessingQueue(newUnitAddress);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Operation(summary = "Post a SPG survey new standalone address outcome to the FWMT Gateway")
  @PostMapping(value = "/spgOutcome/standaloneAddress/new", produces = {"application/json"})
  public ResponseEntity<Void> spgNewStandalone(@RequestBody SPGNewStandaloneAddress newStandaloneAddress) {
    gatewayEventManager.triggerEvent("N/A", COMET_SPG_STANDALONE_OUTCOME_RECEIVED,
        "transactionId", newStandaloneAddress.getTransactionId().toString(),
        "Survey type", "SPG",
        "Primary Outcome", newStandaloneAddress.getPrimaryOutcomeDescription(),
        "Secondary Outcome", newStandaloneAddress.getSecondaryOutcomeDescription(),
        "Outcome code", newStandaloneAddress.getOutcomeCode(),
        "SPGNewStandaloneAddress", newStandaloneAddress.toString());
    outcomePreprocessingProducer.sendSpgNewStandaloneAddress(newStandaloneAddress);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}

