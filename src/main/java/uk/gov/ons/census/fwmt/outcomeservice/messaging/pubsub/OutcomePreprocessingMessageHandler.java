package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
  private final ReadWriteLock processingLock = new ReentrantReadWriteLock();

  private boolean processingEnabled = true;

  public void handle(BasicAcknowledgeablePubsubMessage originalMessage) {
    processingLock.readLock().lock();
    try {
      if (!processingEnabled) {
        originalMessage.ack();
        return;
      }
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
    } finally {
      processingLock.readLock().unlock();
    }
  }

  public void pause() {
    processingLock.writeLock().lock();
    try {
      processingEnabled = false;
    } finally {
      processingLock.writeLock().unlock();
    }
  }

  public void resume() {
    processingLock.writeLock().lock();
    try {
      processingEnabled = true;
    } finally {
      processingLock.writeLock().unlock();
    }
  }
}