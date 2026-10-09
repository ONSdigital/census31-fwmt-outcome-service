package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class OutcomePreprocessingSubscriberLifecycle implements SmartLifecycle {

  private final Object lifecycleMonitor = new Object();
  private final PubSubTemplate pubSubTemplate;
  private final OutcomePreprocessingMessageHandler messageHandler;
  private final String subscription;
  private final Duration stopTimeout;

  private volatile Subscriber subscriber;
  private volatile boolean running;
  private volatile Exception lifecycleFailure;

  public OutcomePreprocessingSubscriberLifecycle(
      PubSubTemplate pubSubTemplate,
      OutcomePreprocessingMessageHandler messageHandler,
      @Value("${app.messaging.pubsub.outcome-preprocessing-subscription}") String subscription,
      @Value("${app.messaging.pubsub.outcome-preprocessing-stop-timeout:30s}") Duration stopTimeout) {
    this.pubSubTemplate = pubSubTemplate;
    this.messageHandler = messageHandler;
    this.subscription = subscription;
    this.stopTimeout = stopTimeout;
  }

  @Override
  public void start() {
    synchronized (lifecycleMonitor) {
      if (lifecycleFailure != null) {
        throw new IllegalStateException("Outcome.Preprocessing subscriber shutdown failed", lifecycleFailure);
      }
      if (running) {
        return;
      }
      subscriber = pubSubTemplate.subscribe(subscription, messageHandler::handle);
      running = true;
    }
  }

  @Override
  public void stop() {
    synchronized (lifecycleMonitor) {
      if (subscriber == null) {
        running = false;
        return;
      }
      try {
        subscriber.stopAsync().awaitTerminated(stopTimeout.toSeconds(), TimeUnit.SECONDS);
        subscriber = null;
        running = false;
      } catch (TimeoutException | RuntimeException ex) {
        lifecycleFailure = ex;
        running = false;
        throw new IllegalStateException("Outcome.Preprocessing subscriber did not terminate", ex);
      }
    }
  }

  @Override
  public void stop(Runnable callback) {
    try {
      stop();
    } finally {
      callback.run();
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }
}