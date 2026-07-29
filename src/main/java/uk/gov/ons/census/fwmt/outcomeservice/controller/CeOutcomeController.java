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
import uk.gov.ons.census.fwmt.common.data.ce.CENewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.ce.CENewUnitAddress;
import uk.gov.ons.census.fwmt.common.data.ce.CEOutcome;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingPublisher;

import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "feature-flags.outcome.surveys.CE", havingValue = "true", matchIfMissing = false)
@Tag(name = "CE Outcomes", description = "CE survey outcome endpoints")
public class CeOutcomeController {

  private static final String COMET_CE_OUTCOME_RECEIVED = "COMET_CE_OUTCOME_RECEIVED";
  private static final String COMET_CE_UNITADDRESS_OUTCOME_RECEIVED = "COMET_CE_UNITADDRESS_OUTCOME_RECEIVED";
  private static final String COMET_CE_STANDALONE_OUTCOME_RECEIVED = "COMET_CE_STANDALONE_OUTCOME_RECEIVED";

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private OutcomePreprocessingPublisher outcomePreprocessingProducer;

  @Operation(summary = "Post a CE survey outcome to the FWMT Gateway")
  @PostMapping(value = "/ceOutcome/{caseID}", produces = {"application/json"})
  public ResponseEntity<Void> ceOutcomeResponse(
      @PathVariable("caseID") String caseId, @RequestBody CEOutcome ceOutcome) {
    gatewayEventManager.triggerEvent(caseId, COMET_CE_OUTCOME_RECEIVED,
        "transactionId", ceOutcome.getTransactionId().toString(),
        "Primary Outcome", ceOutcome.getPrimaryOutcomeDescription(),
        "Secondary Outcome", ceOutcome.getSecondaryOutcomeDescription(),
        "Outcome code", ceOutcome.getOutcomeCode(),
        "CEOutcome", ceOutcome.toString());
    ceOutcome.setCaseId(UUID.fromString(caseId));
    outcomePreprocessingProducer.sendCeOutcomeToPreprocessingQueue(ceOutcome);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Operation(summary = "Post a CE survey new unit address outcome to the FWMT Gateway")
  @PostMapping(value = "/ceOutcome/unitAddress/new", produces = {"application/json"})
  public ResponseEntity<Void> ceNewUnitAddress(@RequestBody CENewUnitAddress newUnitAddress) {
    gatewayEventManager.triggerEvent("N/A", COMET_CE_UNITADDRESS_OUTCOME_RECEIVED,
        "transactionId", newUnitAddress.getTransactionId().toString(),
        "Site case id", String.valueOf(newUnitAddress.getSiteCaseId()),
        "Primary Outcome", newUnitAddress.getPrimaryOutcomeDescription(),
        "Secondary Outcome", newUnitAddress.getSecondaryOutcomeDescription(),
        "Outcome code", newUnitAddress.getOutcomeCode(),
        "CENewUnitAddress", newUnitAddress.toString());
    outcomePreprocessingProducer.sendCeNewUnitAddressToPreprocessingQueue(newUnitAddress);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Operation(summary = "Post a CE survey new standalone address outcome to the FWMT Gateway")
  @PostMapping(value = "/ceOutcome/standaloneAddress/new", produces = {"application/json"})
  public ResponseEntity<Void> ceNewStandalone(@RequestBody CENewStandaloneAddress newStandaloneAddress) {
    gatewayEventManager.triggerEvent("N/A", COMET_CE_STANDALONE_OUTCOME_RECEIVED,
        "transactionId", newStandaloneAddress.getTransactionId().toString(),
        "Survey type", "CE",
        "Primary Outcome", newStandaloneAddress.getPrimaryOutcomeDescription(),
        "Secondary Outcome", newStandaloneAddress.getSecondaryOutcomeDescription(),
        "Outcome code", newStandaloneAddress.getOutcomeCode(),
        "CENewStandaloneAddress", newStandaloneAddress.toString());
    outcomePreprocessingProducer.sendCeNewStandaloneAddressToPreprocessingQueue(newStandaloneAddress);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}

