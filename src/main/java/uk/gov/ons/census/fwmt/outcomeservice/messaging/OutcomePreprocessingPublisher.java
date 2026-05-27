package uk.gov.ons.census.fwmt.outcomeservice.messaging;

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

/**
 * Port for publishing outcomes to the preprocessing lane (Outcome.Preprocessing).
 */
public interface OutcomePreprocessingPublisher {

  void sendSpgOutcomeToPreprocessingQueue(SPGOutcome spgOutcome);

  void sendSpgNewUnitAddressToPreprocessingQueue(SPGNewUnitAddress newUnitAddress);

  void sendSpgNewStandaloneAddress(SPGNewStandaloneAddress newStandaloneAddress);

  void sendCeOutcomeToPreprocessingQueue(CEOutcome ceOutcome);

  void sendCeNewUnitAddressToPreprocessingQueue(CENewUnitAddress newUnitAddress);

  void sendCeNewStandaloneAddressToPreprocessingQueue(CENewStandaloneAddress newStandaloneAddress);

  void sendHHOutcomeToPreprocessingQueue(HHOutcome hhOutcome);

  void sendHHSplitAddressToPreprocessingQueue(HHNewSplitAddress hhNewSplitAddress);

  void sendHHStandaloneAddressToPreprocessingQueue(HHNewStandaloneAddress hhNewStandaloneAddress);

  void sendCcsPropertyListingToPreprocessingQueue(CCSPropertyListingOutcome ccsPropertyListingOutcome);

  void sendCcsInterviewToPreprocessingQueue(CCSInterviewOutcome ccsInterviewOutcome);

  void sendHHStandaloneAddressToPreprocessingQueue(NCOutcome ncOutcome);
}
