package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.messaging.MessagingProperties;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomePreprocessingQueueConfig;

import static uk.gov.ons.census.fwmt.outcomeservice.config.OutcomePreprocessingQueueConfig.OUTCOME_PREPROCESSING_DLQ;

@Slf4j
@Component
public class OutcomeProcessPreprocessingDlq {

  @Autowired
  @Qualifier("OS_RT_GW")
  private ObjectProvider<RabbitTemplate> rabbitTemplate;

  @Autowired(required = false)
  private ObjectProvider<PubSubTemplate> pubSubTemplate;

  @Autowired
  private AmqpAdmin amqpAdmin;

  @Value("${app.messaging.provider:rabbit}")
  private String messagingProvider;

  @Value("${app.messaging.destinations.outcomePreprocessing:Outcome.Preprocessing}")
  private String outcomePreprocessingTopic;

  @Value("${app.messaging.destinations.outcomePreprocessingDlq:Outcome.PreprocessingDLQ}")
  private String outcomePreprocessingDlqTopic;

  @Value("${app.messaging.subscriptions.outcomePreprocessingDlq:outcome-service-Outcome-PreprocessingDLQ}")
  private String outcomePreprocessingDlqSubscription;

  public void processDLQ() throws GatewayException {
    if (MessagingProperties.PROVIDER_PUBSUB.equalsIgnoreCase(messagingProvider)) {
      processDlqPubSub();
      return;
    }

    int messageCount;
    Message message;

    try {
      messageCount = (int) amqpAdmin.getQueueProperties(OUTCOME_PREPROCESSING_DLQ).get("QUEUE_MESSAGE_COUNT");

      for (int i = 0; i < messageCount; i++) {
        message = rabbitTemplate.getObject().receive(OUTCOME_PREPROCESSING_DLQ);

        rabbitTemplate.getObject().send(OutcomePreprocessingQueueConfig.OUTCOME_PREPROCESSING_EXCHANGE,
            OutcomePreprocessingQueueConfig.OUTCOME_PREPROCESSING_ROUTING_KEY, message);
      }
    } catch (NullPointerException e) {
      throw new GatewayException(GatewayException.Fault.BAD_REQUEST, "No messages in queue");
    }
  }

  private void processDlqPubSub() throws GatewayException {
    PubSubTemplate template = pubSubTemplate == null ? null : pubSubTemplate.getIfAvailable();
    if (template == null) {
      throw new GatewayException(
          GatewayException.Fault.SYSTEM_ERROR,
          "Pub/Sub is configured but PubSubTemplate is not available");
    }

    int totalRepublished = 0;

    while (true) {
      var messages = template.pull(outcomePreprocessingDlqSubscription, 500, true);
      if (messages == null || messages.isEmpty()) {
        break;
      }

      for (BasicAcknowledgeablePubsubMessage msg : messages) {
        try {
          template.publish(outcomePreprocessingTopic, msg.getPubsubMessage());
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
