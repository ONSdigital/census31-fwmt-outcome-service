package uk.gov.ons.census.fwmt.outcomeservice.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.ccs.CCSInterviewOutcome;
import uk.gov.ons.census.fwmt.common.data.ccs.CCSPropertyListingOutcome;
import uk.gov.ons.census.fwmt.common.data.ce.CENewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.ce.CENewUnitAddress;
import uk.gov.ons.census.fwmt.common.data.ce.CEOutcome;
import uk.gov.ons.census.fwmt.common.data.household.HHNewSplitAddress;
import uk.gov.ons.census.fwmt.common.data.household.HHNewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.household.HHOutcome;
import uk.gov.ons.census.fwmt.common.data.nc.NCOutcome;
import uk.gov.ons.census.fwmt.common.data.spg.SPGNewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.spg.SPGNewUnitAddress;
import uk.gov.ons.census.fwmt.common.data.spg.SPGOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.outcomeservice.message.OutcomePreprocessingReceiver;

/**
 * Invokes the correct {@link OutcomePreprocessingReceiver} handler for a decoded payload.
 */
@Component
@RequiredArgsConstructor
public class OutcomePreprocessingMessageDispatcher {

  private final OutcomePreprocessingReceiver receiver;

  public void dispatch(Object payload) throws GatewayException {
    if (payload instanceof SPGOutcome spgOutcome) {
      receiver.processMessage(spgOutcome);
    } else if (payload instanceof SPGNewUnitAddress spgNewUnitAddress) {
      receiver.processMessage(spgNewUnitAddress);
    } else if (payload instanceof SPGNewStandaloneAddress spgNewStandaloneAddress) {
      receiver.processMessage(spgNewStandaloneAddress);
    } else if (payload instanceof CEOutcome ceOutcome) {
      receiver.processMessage(ceOutcome);
    } else if (payload instanceof CENewUnitAddress ceNewUnitAddress) {
      receiver.processMessage(ceNewUnitAddress);
    } else if (payload instanceof CENewStandaloneAddress ceNewStandaloneAddress) {
      receiver.processMessage(ceNewStandaloneAddress);
    } else if (payload instanceof HHOutcome hhOutcome) {
      receiver.processMessage(hhOutcome);
    } else if (payload instanceof HHNewSplitAddress hhNewSplitAddress) {
      receiver.processMessage(hhNewSplitAddress);
    } else if (payload instanceof HHNewStandaloneAddress hhNewStandaloneAddress) {
      receiver.processMessage(hhNewStandaloneAddress);
    } else if (payload instanceof CCSPropertyListingOutcome ccsPropertyListingOutcome) {
      receiver.processMessage(ccsPropertyListingOutcome);
    } else if (payload instanceof CCSInterviewOutcome ccsInterviewOutcome) {
      receiver.processMessage(ccsInterviewOutcome);
    } else if (payload instanceof NCOutcome ncOutcome) {
      receiver.processMessage(ncOutcome);
    } else {
      throw new IllegalArgumentException("Unsupported outcome preprocessing payload: " + payload.getClass());
    }
  }
}
