package uk.gov.ons.census.fwmt.outcomeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.outcomeservice.message.OutcomeProcessPreprocessingDlq;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub.OutcomePreprocessingSubscriberLifecycle;

@Controller
@RequiredArgsConstructor
public class DlqController {

  private final OutcomeProcessPreprocessingDlq outcomeProcessPreprocessingDLQ;
  private final OutcomePreprocessingSubscriberLifecycle outcomePreprocessingSubscriberLifecycle;

  @GetMapping("/ProcessDLQ")
  public ResponseEntity<String> startDLQProcessor() throws GatewayException {
    outcomeProcessPreprocessingDLQ.processDLQ();
    return ResponseEntity.ok("DLQ listener started.");
  }

  @GetMapping("/StartPreprocessorListener")
  public ResponseEntity<String> startPreprocessorListener() {
    outcomePreprocessingSubscriberLifecycle.start();
    return ResponseEntity.ok("Preprocessor listener started.");
  }

  @GetMapping("/StopPreprocessorListener")
  public ResponseEntity<String> stopPreprocessorListener() {
    outcomePreprocessingSubscriberLifecycle.stop();
    return ResponseEntity.ok("Preprocessor listener stopped.");
  }
}
