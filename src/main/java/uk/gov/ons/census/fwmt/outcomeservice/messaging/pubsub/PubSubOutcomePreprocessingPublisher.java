package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Retryable;
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
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingJsonCodec;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingPublisher;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = MessagingProperties.PROVIDER, havingValue = MessagingProperties.PROVIDER_PUBSUB)
public class PubSubOutcomePreprocessingPublisher implements OutcomePreprocessingPublisher {

  private final PubSubTemplate pubSubTemplate;
  private final OutcomePreprocessingJsonCodec codec;

  @Value("${app.messaging.destinations.outcomePreprocessing:Outcome.Preprocessing}")
  private String outcomePreprocessingTopic;

  @Override
  @Retryable
  public void sendSpgOutcomeToPreprocessingQueue(SPGOutcome spgOutcome) {
    publish(spgOutcome, true);
  }

  @Override
  @Retryable
  public void sendSpgNewUnitAddressToPreprocessingQueue(SPGNewUnitAddress newUnitAddress) {
    publish(newUnitAddress, true);
  }

  @Override
  @Retryable
  public void sendSpgNewStandaloneAddress(SPGNewStandaloneAddress newStandaloneAddress) {
    publish(newStandaloneAddress, true);
  }

  @Override
  @Retryable
  public void sendCeOutcomeToPreprocessingQueue(CEOutcome ceOutcome) {
    publish(ceOutcome, true);
  }

  @Override
  @Retryable
  public void sendCeNewUnitAddressToPreprocessingQueue(CENewUnitAddress newUnitAddress) {
    publish(newUnitAddress, true);
  }

  @Override
  @Retryable
  public void sendCeNewStandaloneAddressToPreprocessingQueue(CENewStandaloneAddress newStandaloneAddress) {
    publish(newStandaloneAddress, true);
  }

  @Override
  @Retryable
  public void sendHHOutcomeToPreprocessingQueue(HHOutcome hhOutcome) {
    publish(hhOutcome, true);
  }

  @Override
  @Retryable
  public void sendHHSplitAddressToPreprocessingQueue(HHNewSplitAddress hhNewSplitAddress) {
    publish(hhNewSplitAddress, true);
  }

  @Override
  @Retryable
  public void sendHHStandaloneAddressToPreprocessingQueue(HHNewStandaloneAddress hhNewStandaloneAddress) {
    publish(hhNewStandaloneAddress, true);
  }

  @Override
  @Retryable
  public void sendCcsPropertyListingToPreprocessingQueue(CCSPropertyListingOutcome ccsPropertyListingOutcome) {
    publish(ccsPropertyListingOutcome, false);
  }

  @Override
  @Retryable
  public void sendCcsInterviewToPreprocessingQueue(CCSInterviewOutcome ccsInterviewOutcome) {
    publish(ccsInterviewOutcome, false);
  }

  @Override
  @Retryable
  public void sendHHStandaloneAddressToPreprocessingQueue(NCOutcome ncOutcome) {
    publish(ncOutcome, true);
  }

  private void publish(Object payload, boolean withTimestamp) {
    PubsubMessage message = codec.toPubsubMessage(payload, withTimestamp);
    log.debug("Publishing outcome preprocessing message to topic {}", outcomePreprocessingTopic);
    pubSubTemplate.publish(outcomePreprocessingTopic, message);
  }
}
