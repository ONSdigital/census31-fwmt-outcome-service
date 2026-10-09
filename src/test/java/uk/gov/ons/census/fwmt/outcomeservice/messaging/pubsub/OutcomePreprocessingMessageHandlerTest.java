package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingExceptionHandler;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingJsonCodec;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingMessageDispatcher;

@ExtendWith(MockitoExtension.class)
class OutcomePreprocessingMessageHandlerTest {

  @Mock
  private OutcomePreprocessingJsonCodec codec;

  @Mock
  private OutcomePreprocessingMessageDispatcher dispatcher;

  @Mock
  private OutcomePreprocessingExceptionHandler exceptionHandler;

  @Mock
  private BasicAcknowledgeablePubsubMessage originalMessage;

  @Test
  void dispatchesDecodedMessagesAndAcknowledgesTheOriginalMessage() {
    PubsubMessage pubsubMessage = PubsubMessage.newBuilder().build();
    Object payload = new Object();
    when(originalMessage.getPubsubMessage()).thenReturn(pubsubMessage);
    when(codec.fromPubsubMessage(pubsubMessage)).thenReturn(payload);
    OutcomePreprocessingMessageHandler handler =
        new OutcomePreprocessingMessageHandler(codec, dispatcher, exceptionHandler);

    handler.handle(originalMessage);

    verify(dispatcher).dispatch(payload);
    verify(originalMessage).ack();
  }

  @Test
  void routesFailuresThenAcknowledgesTheOriginalMessage() {
    PubsubMessage pubsubMessage = PubsubMessage.newBuilder().build();
    RuntimeException failure = new RuntimeException("unable to decode");
    when(originalMessage.getPubsubMessage()).thenReturn(pubsubMessage);
    when(codec.fromPubsubMessage(pubsubMessage)).thenThrow(failure);
    OutcomePreprocessingMessageHandler handler =
        new OutcomePreprocessingMessageHandler(codec, dispatcher, exceptionHandler);

    handler.handle(originalMessage);

    verify(exceptionHandler).handleFailure(pubsubMessage, failure);
    verify(originalMessage).ack();
  }
}