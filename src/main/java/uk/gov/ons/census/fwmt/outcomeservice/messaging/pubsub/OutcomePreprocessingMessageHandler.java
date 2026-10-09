package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingExceptionHandler;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingJsonCodec;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingMessageDispatcher;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutcomePreprocessingMessageHandler {

  private final OutcomePreprocessingJsonCodec codec;
  private final OutcomePreprocessingMessageDispatcher dispatcher;
  private final OutcomePreprocessingExceptionHandler exceptionHandler;

  public void handle(BasicAcknowledgeablePubsubMessage originalMessage) {
    PubsubMessage pubsubMessage = originalMessage.getPubsubMessage();
    try {
      Object payload = codec.fromPubsubMessage(pubsubMessage);
      dispatcher.dispatch(payload);
      originalMessage.ack();
    } catch (Exception ex) {
      log.error("Failed to process Outcome.Preprocessing Pub/Sub message", ex);
      exceptionHandler.handleFailure(pubsubMessage, ex);
      originalMessage.ack();
    }
  }
}