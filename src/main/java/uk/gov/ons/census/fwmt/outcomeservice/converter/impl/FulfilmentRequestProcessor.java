package uk.gov.ons.census.fwmt.outcomeservice.converter.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.converter.QuestionnaireTypeLookup;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord.GatewayCaseRecordBuilder;
import uk.gov.ons.census.fwmt.outcomeservice.dto.FulfilmentRequestDto;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCaseRecordService;
import uk.gov.ons.census.fwmt.outcomeservice.template.TemplateCreator;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;

import java.text.DateFormat;
import java.util.*;

import static uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceLogConfig.*;
import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.FULFILMENT_REQUESTED;
import uk.gov.ons.ctp.common.domain.Channel;

@Slf4j
@Component("FULFILMENT_REQUESTED")
public class FulfilmentRequestProcessor implements OutcomeServiceProcessor {

  @Autowired
  private DateFormat dateFormat;

  @Autowired
  private ProductReference productReference;

  @Autowired
  private QuestionnaireTypeLookup questionnaireTypeLookup;

  @Autowired
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private GatewayCaseRecordService gatewayCacheService;

  @Override
  public UUID process(OutcomeSuperSetDto outcome, UUID caseIdHolder, String type) throws GatewayException {
    gatewayEventManager.triggerEvent(String.valueOf(outcome.getCaseId()), PROCESSING_OUTCOME,
        SURVEY_TYPE, type,
        PROCESSOR, "FULFILMENT_REQUESTED",
        ORIGINAL_CASE_ID, String.valueOf(outcome.getCaseId()),
        SITE_CASE_ID, (outcome.getSiteCaseId() != null ? String.valueOf(outcome.getSiteCaseId()) : "N/A"));
 
    if (outcome.getFulfilmentRequests() == null) return caseIdHolder;
    UUID caseId = (caseIdHolder != null) ? caseIdHolder : outcome.getCaseId();
    for (FulfilmentRequestDto fulfilmentRequest : outcome.getFulfilmentRequests()) {
      if (!isQuestionnaireLinked(fulfilmentRequest) && fulfilmentRequest.getQuestionnaireType() != null) {
        String eventDateTime = dateFormat.format(outcome.getEventDate());
        Map<String, Object> root = new HashMap<>();
        root.put("outcome", outcome);
        root.put("caseId", caseId);
        root.put("eventDate", eventDateTime);
        String outcomeEvent =
            createQuestionnaireRequiredByPostEvent(root, fulfilmentRequest, String.valueOf(caseId), type);

        gatewayOutcomeProducer.sendOutcome(outcomeEvent, String.valueOf(outcome.getTransactionId()),
            GatewayOutcomeQueueConfig.GATEWAY_FULFILMENT_REQUEST_ROUTING_KEY);

        gatewayEventManager.triggerEvent(String.valueOf(caseIdHolder), OUTCOME_SENT,
            SURVEY_TYPE, type,
            TEMPLATE_TYPE, FULFILMENT_REQUESTED.toString(),
            TRANSACTION_ID, outcome.getTransactionId().toString(),
            ROUTING_KEY, GatewayOutcomeQueueConfig.GATEWAY_FULFILMENT_REQUEST_ROUTING_KEY);
      }
    }
    return caseId;
  }

  private String createQuestionnaireRequiredByPostEvent(
      Map<String, Object> root,
      FulfilmentRequestDto fulfilmentRequest,
      String caseId,
      String type)
      throws GatewayException {
    String individualCaseId = "";

    Product product = getProductFromQuestionnaireType(fulfilmentRequest);
    if (product.getIndividual() && type.equals("HH")) {
      individualCaseId = String.valueOf(UUID.randomUUID());
      root.put("individualCaseId", individualCaseId);
      root.put("surveyType", type);
    }
    root.put("packcode", product.getFulfilmentCode());
    root.put("requesterTitle", fulfilmentRequest.getRequesterTitle());
    root.put("requesterForename", fulfilmentRequest.getRequesterForename());
    root.put("requesterSurname", fulfilmentRequest.getRequesterSurname());
    root.put("requesterPhone", fulfilmentRequest.getRequesterPhone());

    cacheData(caseId, individualCaseId);

    return TemplateCreator.createOutcomeMessage(FULFILMENT_REQUESTED, root);
  }

  private Product getProductFromQuestionnaireType(FulfilmentRequestDto fulfilmentRequest)
      throws GatewayException {
    String questionnaireType = fulfilmentRequest.getQuestionnaireType();
    String packCode = questionnaireTypeLookup.getPackCode(questionnaireType);
    if (packCode == null) {
      throw new GatewayException(
          GatewayException.Fault.SYSTEM_ERROR,
          "Unknown questionnaireType: " + questionnaireType);
    }

    Product product = new Product();
    List<Channel> requestChannels = Collections.singletonList(Channel.FIELD);

    product.setRequestChannels(requestChannels);
    product.setFulfilmentCode(packCode);

    List<Product> productList;
    try {
      productList = productReference.searchProducts(product);
    } catch (RuntimeException e) {
      throw new GatewayException(
          GatewayException.Fault.SYSTEM_ERROR,
          e,
          "Product lookup failed for questionnaireType: "
              + questionnaireType
              + ", packCode: "
              + packCode);
    }

    if (productList.size() != 1) {
      throw new GatewayException(
          GatewayException.Fault.SYSTEM_ERROR,
          "Failed to find 1 product using questionnaireType: "
              + questionnaireType
              + ", packCode: "
              + packCode
              + ", matches: "
              + productList.size());
    }
    return productList.get(0);
  }

  private boolean isQuestionnaireLinked(FulfilmentRequestDto fulfilmentRequest) {
    return (fulfilmentRequest.getQuestionnaireID() != null);
  }

  private void cacheData(String caseId, String individualCaseId) {
    GatewayCaseRecord cache = gatewayCacheService.getById(caseId);
    GatewayCaseRecordBuilder builder ;
    if (cache == null) builder = GatewayCaseRecord.builder();
    else builder = cache.toBuilder();

    if (!individualCaseId.equals("")) {
      gatewayCacheService.save(builder
          .caseId(caseId)
          .delivered(true)
          .individualCaseId(individualCaseId)
          .build());
    } else {
      gatewayCacheService.save(builder
          .caseId(caseId)
          .delivered(true)
          .build());
    }
  }
}