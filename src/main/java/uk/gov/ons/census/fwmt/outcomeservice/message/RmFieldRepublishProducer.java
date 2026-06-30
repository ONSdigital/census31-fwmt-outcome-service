package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.messaging.FieldWorkerInstructionJsonCodec;
import uk.gov.ons.census.fwmt.common.messaging.MessagingProperties;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtCancelActionInstruction;

@Service
public class RmFieldRepublishProducer {

  private final FieldWorkerInstructionJsonCodec fieldWorkerInstructionJsonCodec = new FieldWorkerInstructionJsonCodec();

  @Autowired
  @Qualifier("feedbackRabbitTemplate")
  private ObjectProvider<RabbitTemplate> rabbitTemplate;

  @Autowired(required = false)
  private ObjectProvider<PubSubTemplate> pubSubTemplate;

  @Value("${app.messaging.provider:rabbit}")
  private String messagingProvider;

  @Value("${app.messaging.destinations.rmField:RM.Field}")
  private String rmFieldTopic;

  public void republish(FwmtCancelActionInstruction fieldworkFollowup) {
    if (MessagingProperties.PROVIDER_PUBSUB.equalsIgnoreCase(messagingProvider)) {
      publishToPubSub(fieldworkFollowup);
    } else {
      publishToRabbit(fieldworkFollowup);
    }
  }

  public void republish(FwmtActionInstruction fieldworkFollowup) {
    if (MessagingProperties.PROVIDER_PUBSUB.equalsIgnoreCase(messagingProvider)) {
      publishToPubSub(fieldworkFollowup);
    } else {
      publishToRabbit(fieldworkFollowup);
    }
  }

  private void publishToRabbit(Object fieldworkFollowup) {
    RabbitTemplate template = rabbitTemplate.getIfAvailable();
    if (template == null) {
      throw new IllegalStateException(
          "Rabbit is configured but feedbackRabbitTemplate is not available (app.messaging.provider=rabbit)");
    }
    template.convertAndSend("RM.Field", fieldworkFollowup, timestampPostProcessor());
  }

  private MessagePostProcessor timestampPostProcessor() {
    return new MessagePostProcessor() {
      @Override
      public Message postProcessMessage(Message message) throws AmqpException {
        long epochMilli = Instant.now().toEpochMilli();
        message.getMessageProperties().setTimestamp(new Date(epochMilli));
        return message;
      }
    };
  }

  private void publishToPubSub(Object payload) {
    PubSubTemplate template = pubSubTemplate.getIfAvailable();
    if (template == null) {
      throw new IllegalStateException("Pub/Sub is configured but PubSubTemplate is not available");
    }

    PubsubMessage message = fieldWorkerInstructionJsonCodec.toPubsubMessage(payload, true);
    template.publish(rmFieldTopic, message);
  }
}
