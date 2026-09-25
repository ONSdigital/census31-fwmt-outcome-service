package uk.gov.ons.census.fwmt.outcomeservice.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.FulfilmentRequestProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.dto.FulfilmentRequestDto;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.EventDictionaryMessageFactory;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCaseRecordService;
import uk.gov.ons.ctp.common.domain.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;

@ExtendWith(MockitoExtension.class)
class FulfilmentRequestProcessorTest {

  @InjectMocks
  private FulfilmentRequestProcessor fulfilmentRequestProcessor;

  @Mock
  private ProductReference productReference;

  @Mock
  private QuestionnaireTypeLookup questionnaireTypeLookup;

  @Mock
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Mock
  private GatewayEventManager gatewayEventManager;

  @Mock
  private GatewayCaseRecordService gatewayCacheService;

  @Mock
  private EventDictionaryMessageFactory eventDictionaryMessageFactory;

  @Captor
  private ArgumentCaptor<GatewayCaseRecord> cacheCaptor;

  @Test
  @DisplayName("Publishes one Event Dictionary fulfilment request per eligible non-linked request")
  void publishesEligibleNonLinkedRequest() throws GatewayException {
    OutcomeSuperSetDto outcome = new OutcomeSuperSetDto();
    outcome.setCaseId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    outcome.setTransactionId(UUID.fromString("123e4567-e89b-12d3-a456-426614174002"));

    FulfilmentRequestDto request = FulfilmentRequestDto.builder()
        .questionnaireType("I1")
        .requesterTitle("Ms")
        .requesterForename("Jo")
        .requesterSurname("Smith")
        .requesterPhone("+447700900123")
        .build();
    outcome.setFulfilmentRequests(List.of(request));

    Product product = new Product();
    product.setFulfilmentCode("P_OR_I1");
    product.setIndividual(true);
    product.setDeliveryChannel(DeliveryChannel.POST);

    when(questionnaireTypeLookup.getPackCode("I1")).thenReturn("P_OR_I1");
    when(productReference.searchProducts(any(Product.class))).thenReturn(List.of(product));
    when(eventDictionaryMessageFactory.buildFulfilmentRequest(
        eq(outcome.getCaseId().toString()),
        eq("P_OR_I1"),
        anyString(),
        eq(true),
        eq("Ms"),
        eq("Jo"),
        eq("Smith"),
        eq(false),
        eq("+447700900123"))).thenReturn("{}");

    fulfilmentRequestProcessor.process(outcome, null, "HH");

    verify(gatewayOutcomeProducer).sendOutcome(eq("{}"), eq(outcome.getTransactionId().toString()), anyString());
    verify(gatewayCacheService).save(cacheCaptor.capture());
    verify(eventDictionaryMessageFactory).buildFulfilmentRequest(
        eq(outcome.getCaseId().toString()),
        eq("P_OR_I1"),
        anyString(),
        eq(true),
        eq("Ms"),
        eq("Jo"),
        eq("Smith"),
        eq(false),
        eq("+447700900123"));
  }

  @Test
  @DisplayName("Skips linked fulfilment requests")
  void skipsLinkedRequests() throws GatewayException {
    OutcomeSuperSetDto outcome = new OutcomeSuperSetDto();
    outcome.setCaseId(UUID.fromString("123e4567-e89b-12d3-a456-426614174003"));
    outcome.setTransactionId(UUID.fromString("123e4567-e89b-12d3-a456-426614174004"));
    outcome.setFulfilmentRequests(Collections.singletonList(FulfilmentRequestDto.builder()
        .questionnaireType("HUAC1")
        .questionnaireID("linked-qid")
        .build()));

    fulfilmentRequestProcessor.process(outcome, null, "HH");

    verify(gatewayOutcomeProducer, never()).sendOutcome(anyString(), anyString(), anyString());
    verifyNoInteractions(eventDictionaryMessageFactory);
  }
}