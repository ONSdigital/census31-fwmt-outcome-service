package uk.gov.ons.census.fwmt.outcomeservice.messaging;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Routes failed Outcome.Preprocessing messages to retry or DLQ instead of nacking
 * (which would redeliver indefinitely on the same subscription).
 */
@Slf4j
@Component
public class OutcomePreprocessingExceptionHandler {

  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Value("${app.messaging.maxRetryCount:5}")
  private int maxRetryCount;

  @Value("${app.messaging.destinations.outcomePreprocessing:Outcome.Preprocessing}")
  private String outcomePreprocessingTopic;

  @Value("${app.messaging.destinations.outcomePreprocessingDlq:Outcome.PreprocessingDLQ}")
  private String outcomePreprocessingDlqTopic;

  public void handleFailure(PubsubMessage message, Exception ex) {
    if (isTransient(ex) && parseRetryCount(message) < maxRetryCount) {
      int nextRetryCount = parseRetryCount(message) + 1;
      publishPubSub(
          outcomePreprocessingTopic,
          message,
          Map.of("retryCount", String.valueOf(nextRetryCount)));
      log.warn(
          "Republished Outcome.Preprocessing message for retry {}/{}: {}",
          nextRetryCount,
          maxRetryCount,
          ex.getMessage());
      return;
    }

    publishPubSub(outcomePreprocessingDlqTopic, message, Map.of());
    log.error("Moved Outcome.Preprocessing message to DLQ: {}", ex.getMessage(), ex);
  }

  private boolean isTransient(Exception ex) {
    if (ex instanceof RestClientException) {
      return true;
    }
    Throwable cause = ex.getCause();
    while (cause != null) {
      if (cause instanceof RestClientException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private Integer parseRetryCount(PubsubMessage message) {
    try {
      return Integer.parseInt(message.getAttributesOrDefault("retryCount", "0"));
    } catch (RuntimeException ex) {
      return 0;
    }
  }

  private void publishPubSub(String topic, PubsubMessage original, Map<String, String> extraAttributes) {
    PubsubMessage.Builder builder = PubsubMessage.newBuilder()
        .setData(original.getData() == null ? ByteString.EMPTY : original.getData())
        .putAllAttributes(original.getAttributesMap());

    if (extraAttributes != null && !extraAttributes.isEmpty()) {
      builder.putAllAttributes(extraAttributes);
    }

    pubSubTemplate.publish(topic, builder.build());
  }
}
