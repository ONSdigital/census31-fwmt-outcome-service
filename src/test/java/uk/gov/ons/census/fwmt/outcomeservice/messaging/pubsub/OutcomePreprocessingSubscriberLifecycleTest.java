package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutcomePreprocessingSubscriberLifecycleTest {

  private static final String SUBSCRIPTION = "outcome-service-Outcome-Preprocessing";

  @Mock
  private PubSubTemplate pubSubTemplate;

  @Mock
  private OutcomePreprocessingMessageHandler messageHandler;

  @Mock
  private Subscriber firstSubscriber;

  @Mock
  private Subscriber secondSubscriber;

  @Mock
  private DefaultSubscriberFactory subscriberFactory;

  private OutcomePreprocessingSubscriberLifecycle lifecycle;

  @BeforeEach
  void setUp() {
    lifecycle = new OutcomePreprocessingSubscriberLifecycle(
        pubSubTemplate, messageHandler, SUBSCRIPTION, Duration.ofSeconds(30));
  }

  @Test
  void startsOnlyOneSubscriberWhileAlreadyRunning() {
    when(pubSubTemplate.subscribe(eq(SUBSCRIPTION), any())).thenReturn(firstSubscriber);

    lifecycle.start();
    lifecycle.start();

    verify(pubSubTemplate, times(1)).subscribe(eq(SUBSCRIPTION), any());
    assertThat(lifecycle.isRunning()).isTrue();
  }

  @Test
  void configuresOneReusableExecutorProviderForTheSubscriberFactory() {
    when(pubSubTemplate.getSubscriberFactory()).thenReturn(subscriberFactory);
    when(pubSubTemplate.subscribe(eq(SUBSCRIPTION), any())).thenReturn(firstSubscriber);

    lifecycle.start();

    verify(subscriberFactory).setExecutorProvider(any());
    verify(subscriberFactory).setSystemExecutorProvider(any());
  }

  @Test
  void pausesAndResumesMessageProcessingWithoutReplacingTheSubscriber() {
    when(pubSubTemplate.subscribe(eq(SUBSCRIPTION), any())).thenReturn(firstSubscriber);
    lifecycle.start();
    clearInvocations(messageHandler);

    lifecycle.pause();
    lifecycle.resume();

    verify(messageHandler).pause();
    verify(messageHandler).resume();
    verify(pubSubTemplate, times(1)).subscribe(eq(SUBSCRIPTION), any());
  }

  @Test
  void stopsAndAwaitsSubscriberTermination() throws TimeoutException {
    when(pubSubTemplate.subscribe(eq(SUBSCRIPTION), any())).thenReturn(firstSubscriber);
    when(firstSubscriber.stopAsync()).thenReturn(firstSubscriber);
    doNothing().when(firstSubscriber).awaitTerminated(30, TimeUnit.SECONDS);
    lifecycle.start();

    lifecycle.stop();

    InOrder inOrder = inOrder(firstSubscriber);
    inOrder.verify(firstSubscriber).stopAsync();
    inOrder.verify(firstSubscriber).awaitTerminated(30, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isFalse();
  }

  @Test
  void restartsOnlyAfterThePreviousSubscriberTerminates() throws TimeoutException {
    when(pubSubTemplate.subscribe(eq(SUBSCRIPTION), any()))
        .thenReturn(firstSubscriber, secondSubscriber);
    when(firstSubscriber.stopAsync()).thenReturn(firstSubscriber);
    doNothing().when(firstSubscriber).awaitTerminated(30, TimeUnit.SECONDS);
    lifecycle.start();

    lifecycle.stop();
    lifecycle.start();

    InOrder inOrder = inOrder(pubSubTemplate, firstSubscriber);
    inOrder.verify(pubSubTemplate).subscribe(eq(SUBSCRIPTION), any());
    inOrder.verify(firstSubscriber).stopAsync();
    inOrder.verify(firstSubscriber).awaitTerminated(30, TimeUnit.SECONDS);
    inOrder.verify(pubSubTemplate).subscribe(eq(SUBSCRIPTION), any());
  }

  @Test
  void rejectsRestartWhenSubscriberDoesNotTerminate() throws TimeoutException {
    when(pubSubTemplate.subscribe(eq(SUBSCRIPTION), any())).thenReturn(firstSubscriber);
    when(firstSubscriber.stopAsync()).thenReturn(firstSubscriber);
    doThrow(new TimeoutException("subscriber did not terminate"))
      .when(firstSubscriber).awaitTerminated(30, TimeUnit.SECONDS);
    lifecycle.start();

    assertThatIllegalStateException().isThrownBy(lifecycle::stop);
    assertThatIllegalStateException().isThrownBy(lifecycle::start);
    verify(pubSubTemplate, times(1)).subscribe(eq(SUBSCRIPTION), any());
  }
}