package uk.gov.ons.census.fwmt.outcomeservice.converter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.AddressNotValidProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.EventDictionaryMessageFactory;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;

@ExtendWith(MockitoExtension.class)
class AddressNotValidProcessorTest {

  @InjectMocks
  private AddressNotValidProcessor addressNotValidProcessor;

  @Mock
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Mock
  private GatewayEventManager gatewayEventManager;

  @Mock
  private ReasonCodeLookup reasonCodeLookup;

  @Mock
  private EventDictionaryMessageFactory eventDictionaryMessageFactory;

  @Test
  void publishesDictionaryMessageToDedicatedAddressNotValidRoute() throws GatewayException {
    UUID caseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    UUID transactionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");
    OutcomeSuperSetDto outcome = new OutcomeSuperSetDto();
    outcome.setCaseId(caseId);
    outcome.setTransactionId(transactionId);
    outcome.setOutcomeCode("01-03-01");

    when(reasonCodeLookup.getLookup("01-03-01")).thenReturn("DERELICT");
    when(eventDictionaryMessageFactory.buildAddressNotValid("DERELICT", caseId.toString()))
        .thenReturn("{\"dictionary\":true}");

    addressNotValidProcessor.process(outcome, null, "HH");

    verify(eventDictionaryMessageFactory).buildAddressNotValid("DERELICT", caseId.toString());
    verify(gatewayOutcomeProducer).sendOutcome(
        eq("{\"dictionary\":true}"),
        eq(transactionId.toString()),
        eq(GatewayOutcomeQueueConfig.GATEWAY_ADDRESS_NOT_VALID_ROUTING_KEY));
  }
}