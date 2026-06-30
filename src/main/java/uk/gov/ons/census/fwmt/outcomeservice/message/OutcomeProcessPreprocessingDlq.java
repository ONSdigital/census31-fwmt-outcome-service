package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;

@Slf4j
@Component
public class OutcomeProcessPreprocessingDlq {

  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Value("${app.messaging.destinations.outcomePreprocessing:Outcome.Preprocessing}")
  private String outcomePreprocessingTopic;

  @Value("${app.messaging.destinations.outcomePreprocessingDlq:Outcome.PreprocessingDLQ}")
  private String outcomePreprocessingDlqTopic;

  @Value("${app.messaging.subscriptions.outcomePreprocessingDlq:outcome-service-Outcome-PreprocessingDLQ}")
  private String outcomePreprocessingDlqSubscription;

  public void processDLQ() throws GatewayException {
    int totalRepublished = 0;

    while (true) {
      var messages = pubSubTemplate.pull(outcomePreprocessingDlqSubscription, 500, true);
      if (messages == null || messages.isEmpty()) {
        break;
      }

      for (BasicAcknowledgeablePubsubMessage msg : messages) {
        try {
          pubSubTemplate.publish(outcomePreprocessingTopic, msg.getPubsubMessage());
          msg.ack();
          totalRepublished++;
        } catch (Exception e) {
          // Leave the message unacked so it can be retried later.
          log.warn(
              "Failed to republish Outcome preprocessing DLQ message from subscription={} topic={} to topic={}",
              outcomePreprocessingDlqSubscription,
              outcomePreprocessingDlqTopic,
              outcomePreprocessingTopic,
              e);
        }
      }
    }

    if (totalRepublished == 0) {
      throw new GatewayException(GatewayException.Fault.BAD_REQUEST, "No messages in queue");
    }
  }
}
