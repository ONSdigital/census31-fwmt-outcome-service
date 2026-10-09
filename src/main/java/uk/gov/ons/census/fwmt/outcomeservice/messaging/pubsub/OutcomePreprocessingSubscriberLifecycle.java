package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
  private final int executorThreads;

  private volatile Subscriber subscriber;
  private volatile boolean running;
  private volatile Exception lifecycleFailure;
  private ScheduledExecutorService subscriberExecutor;
  private ExecutorProvider subscriberExecutorProvider;

  OutcomePreprocessingSubscriberLifecycle(
      PubSubTemplate pubSubTemplate,
      OutcomePreprocessingMessageHandler messageHandler,
      String subscription,
      Duration stopTimeout) {
    this(pubSubTemplate, messageHandler, subscription, stopTimeout, 4);
  }

  public OutcomePreprocessingSubscriberLifecycle(
      PubSubTemplate pubSubTemplate,
      OutcomePreprocessingMessageHandler messageHandler,
      @Value("${app.messaging.pubsub.outcome-preprocessing-subscription}") String subscription,
      @Value("${app.messaging.pubsub.outcome-preprocessing-stop-timeout:30s}") Duration stopTimeout,
      @Value("${app.messaging.pubsub.outcome-preprocessing-executor-threads:4}") int executorThreads) {
    this.pubSubTemplate = pubSubTemplate;
    this.messageHandler = messageHandler;
    this.subscription = subscription;
    this.stopTimeout = stopTimeout;
    this.executorThreads = executorThreads;
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
      configureSubscriberExecutor();
      messageHandler.resume();
      subscriber = pubSubTemplate.subscribe(subscription, messageHandler::handle);
      running = true;
    }
  }

  public void pause() {
    synchronized (lifecycleMonitor) {
      messageHandler.pause();
    }
  }

  public void resume() {
    synchronized (lifecycleMonitor) {
      if (running) {
        messageHandler.resume();
        return;
      }
      start();
    }
  }

  @Override
  public void stop() {
    synchronized (lifecycleMonitor) {
      messageHandler.pause();
      if (subscriber == null) {
        running = false;
        return;
      }
      try {
        subscriber.stopAsync().awaitTerminated(stopTimeout.toSeconds(), TimeUnit.SECONDS);
        subscriber = null;
        running = false;
        shutdownSubscriberExecutor();
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

  private void configureSubscriberExecutor() {
    if (subscriberExecutor != null && !subscriberExecutor.isShutdown()) {
      return;
    }
    subscriberExecutor = Executors.newScheduledThreadPool(executorThreads);
    subscriberExecutorProvider = FixedExecutorProvider.create(subscriberExecutor);
    if (pubSubTemplate.getSubscriberFactory() instanceof DefaultSubscriberFactory subscriberFactory) {
      subscriberFactory.setExecutorProvider(subscriberExecutorProvider);
      subscriberFactory.setSystemExecutorProvider(subscriberExecutorProvider);
    }
  }

  private void shutdownSubscriberExecutor() {
    if (subscriberExecutor == null) {
      return;
    }
    subscriberExecutor.shutdown();
    try {
      if (!subscriberExecutor.awaitTermination(stopTimeout.toSeconds(), TimeUnit.SECONDS)) {
        subscriberExecutor.shutdownNow();
      }
    } catch (InterruptedException ex) {
      subscriberExecutor.shutdownNow();
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while stopping Outcome.Preprocessing executor", ex);
    }
  }
}