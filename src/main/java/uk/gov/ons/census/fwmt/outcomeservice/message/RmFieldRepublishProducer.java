package uk.gov.ons.census.fwmt.outcomeservice.message;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.messaging.FieldWorkerInstructionJsonCodec;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtCancelActionInstruction;

@Service
public class RmFieldRepublishProducer {

  private final FieldWorkerInstructionJsonCodec fieldWorkerInstructionJsonCodec = new FieldWorkerInstructionJsonCodec();

  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Value("${app.messaging.destinations.rmField:RM.Field}")
  private String rmFieldTopic;

  public void republish(FwmtCancelActionInstruction fieldworkFollowup) {
    publishToPubSub(fieldworkFollowup);
  }

  public void republish(FwmtActionInstruction fieldworkFollowup) {
    publishToPubSub(fieldworkFollowup);
  }

  private void publishToPubSub(Object payload) {
    PubsubMessage message = fieldWorkerInstructionJsonCodec.toPubsubMessage(payload, true);
    pubSubTemplate.publish(rmFieldTopic, message);
  }
}
