package uk.gov.ons.census.fwmt.outcomeservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.ccs.CCSInterviewOutcome;
import uk.gov.ons.census.fwmt.common.data.ccs.CCSPropertyListingOutcome;
import uk.gov.ons.census.fwmt.common.data.ce.CENewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.ce.CENewUnitAddress;
import uk.gov.ons.census.fwmt.common.data.ce.CEOutcome;
import uk.gov.ons.census.fwmt.common.data.household.HHNewSplitAddress;
import uk.gov.ons.census.fwmt.common.data.household.HHNewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.household.HHOutcome;
import uk.gov.ons.census.fwmt.common.data.nc.NCOutcome;
import uk.gov.ons.census.fwmt.common.data.spg.SPGNewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.spg.SPGNewUnitAddress;
import uk.gov.ons.census.fwmt.common.data.spg.SPGOutcome;

/**
 * JSON codec aligned with the Rabbit {@code Jackson2JsonMessageConverter} type ids for this lane.
 */
@Component
public class OutcomePreprocessingJsonCodec {

  public static final String TYPE_ID_HEADER = "__TypeId__";
  public static final String TIMESTAMP_HEADER = "timestamp";

  private final ObjectMapper objectMapper;
  private final Map<String, Class<?>> typeIds;

  public OutcomePreprocessingJsonCodec() {
    objectMapper = new ObjectMapper();
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    typeIds = outcomePreprocessingTypeIds();
  }

  public PubsubMessage toPubsubMessage(Object payload, boolean withTimestamp) {
    try {
      String typeId = payload.getClass().getName();
      String json = objectMapper.writeValueAsString(payload);
      PubsubMessage.Builder builder = PubsubMessage.newBuilder()
          .setData(ByteString.copyFromUtf8(json))
          .putAttributes(TYPE_ID_HEADER, typeId);
      if (withTimestamp) {
        builder.putAttributes(TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()));
      }
      return builder.build();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to encode outcome preprocessing message", e);
    }
  }

  public Object fromPubsubMessage(PubsubMessage message) {
    String typeId = message.getAttributesOrDefault(TYPE_ID_HEADER, "");
    if (typeId.isEmpty()) {
      throw new IllegalStateException("Missing " + TYPE_ID_HEADER + " attribute on Pub/Sub message");
    }
    Class<?> type = typeIds.get(typeId);
    if (type == null) {
      throw new IllegalStateException("Unknown outcome preprocessing type id: " + typeId);
    }
    try {
      return objectMapper.readValue(message.getData().toStringUtf8(), type);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to decode outcome preprocessing message", e);
    }
  }

  private static Map<String, Class<?>> outcomePreprocessingTypeIds() {
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put(SPGOutcome.class.getName(), SPGOutcome.class);
    idClassMapping.put(SPGNewUnitAddress.class.getName(), SPGNewUnitAddress.class);
    idClassMapping.put(SPGNewStandaloneAddress.class.getName(), SPGNewStandaloneAddress.class);
    idClassMapping.put(CEOutcome.class.getName(), CEOutcome.class);
    idClassMapping.put(CENewUnitAddress.class.getName(), CENewUnitAddress.class);
    idClassMapping.put(CENewStandaloneAddress.class.getName(), CENewStandaloneAddress.class);
    idClassMapping.put(HHOutcome.class.getName(), HHOutcome.class);
    idClassMapping.put(HHNewSplitAddress.class.getName(), HHNewSplitAddress.class);
    idClassMapping.put(HHNewStandaloneAddress.class.getName(), HHNewStandaloneAddress.class);
    idClassMapping.put(CCSPropertyListingOutcome.class.getName(), CCSPropertyListingOutcome.class);
    idClassMapping.put(CCSInterviewOutcome.class.getName(), CCSInterviewOutcome.class);
    idClassMapping.put(NCOutcome.class.getName(), NCOutcome.class);
    return idClassMapping;
  }
}
