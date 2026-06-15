package uk.gov.ons.census.fwmt.outcomeservice.messaging;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import uk.gov.ons.census.fwmt.common.error.GatewayException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutcomePreprocessingExceptionHandlerTest {

  @Captor
  private ArgumentCaptor<PubsubMessage> pubsubMessageArgumentCaptor;

  @InjectMocks
  private OutcomePreprocessingExceptionHandler exceptionHandler;

  @Mock
  private PubSubTemplate pubSubTemplate;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(exceptionHandler, "maxRetryCount", 5);
    ReflectionTestUtils.setField(exceptionHandler, "outcomePreprocessingTopic", "Outcome.Preprocessing");
    ReflectionTestUtils.setField(exceptionHandler, "outcomePreprocessingDlqTopic", "Outcome.PreprocessingDLQ");
  }

  @DisplayName("GatewayException routes message to DLQ")
  @Test
  void shouldRouteGatewayExceptionToDlq() {
    PubsubMessage message = createPubsubMessage(null);
    exceptionHandler.handleFailure(
        message, new GatewayException(GatewayException.Fault.SYSTEM_ERROR, "Case did not exist in cache"));

    verify(pubSubTemplate).publish(eq("Outcome.PreprocessingDLQ"), pubsubMessageArgumentCaptor.capture());
    assertEquals("payload", pubsubMessageArgumentCaptor.getValue().getData().toStringUtf8());
  }

  @DisplayName("RestClientException republishes with incremented retryCount")
  @Test
  void shouldRetryTransientFailure() {
    PubsubMessage message = createPubsubMessage(1);
    exceptionHandler.handleFailure(message, new RestClientException("timeout"));

    verify(pubSubTemplate).publish(eq("Outcome.Preprocessing"), pubsubMessageArgumentCaptor.capture());
    assertEquals("2", pubsubMessageArgumentCaptor.getValue().getAttributesOrDefault("retryCount", "0"));
  }

  @DisplayName("Transient failure exceeding retry limit routes to DLQ")
  @Test
  void shouldRouteTransientFailureToDlqWhenRetryLimitReached() {
    PubsubMessage message = createPubsubMessage(5);
    exceptionHandler.handleFailure(message, new RestClientException("timeout"));

    verify(pubSubTemplate).publish(eq("Outcome.PreprocessingDLQ"), any(PubsubMessage.class));
  }

  private static PubsubMessage createPubsubMessage(Integer retryCount) {
    PubsubMessage.Builder builder = PubsubMessage.newBuilder()
        .setData(ByteString.copyFromUtf8("payload"));
    if (retryCount != null) {
      builder.putAttributes("retryCount", String.valueOf(retryCount));
    }
    return builder.build();
  }
}
