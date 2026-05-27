package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import uk.gov.ons.census.fwmt.common.messaging.MessagingProperties;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingPublisher;

@Component
@ConditionalOnProperty(name = MessagingProperties.PROVIDER, havingValue = MessagingProperties.PROVIDER_PUBSUB)
public class PubSubOutcomePreprocessingPublisher implements OutcomePreprocessingPublisher {

  private static final String NOT_IMPLEMENTED =
      "Pub/Sub Outcome.Preprocessing publish is not implemented (Stage 2). Set app.messaging.provider=rabbit.";

  @Override
  public void sendSpgOutcomeToPreprocessingQueue(SPGOutcome spgOutcome) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendSpgNewUnitAddressToPreprocessingQueue(SPGNewUnitAddress newUnitAddress) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendSpgNewStandaloneAddress(SPGNewStandaloneAddress newStandaloneAddress) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendCeOutcomeToPreprocessingQueue(CEOutcome ceOutcome) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendCeNewUnitAddressToPreprocessingQueue(CENewUnitAddress newUnitAddress) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendCeNewStandaloneAddressToPreprocessingQueue(CENewStandaloneAddress newStandaloneAddress) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendHHOutcomeToPreprocessingQueue(HHOutcome hhOutcome) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendHHSplitAddressToPreprocessingQueue(HHNewSplitAddress hhNewSplitAddress) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendHHStandaloneAddressToPreprocessingQueue(HHNewStandaloneAddress hhNewStandaloneAddress) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendCcsPropertyListingToPreprocessingQueue(CCSPropertyListingOutcome ccsPropertyListingOutcome) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendCcsInterviewToPreprocessingQueue(CCSInterviewOutcome ccsInterviewOutcome) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendHHStandaloneAddressToPreprocessingQueue(NCOutcome ncOutcome) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }
}
