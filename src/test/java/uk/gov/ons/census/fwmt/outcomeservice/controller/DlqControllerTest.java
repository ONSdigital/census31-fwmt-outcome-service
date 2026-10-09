package uk.gov.ons.census.fwmt.outcomeservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.census.fwmt.outcomeservice.message.OutcomeProcessPreprocessingDlq;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub.OutcomePreprocessingSubscriberLifecycle;

@ExtendWith(MockitoExtension.class)
class DlqControllerTest {

  @Mock
  private OutcomeProcessPreprocessingDlq outcomeProcessPreprocessingDlq;

  @Mock
  private OutcomePreprocessingSubscriberLifecycle preprocessorLifecycle;

  @Test
  void startsTheOwnedPreprocessorLifecycle() {
    DlqController controller = new DlqController(outcomeProcessPreprocessingDlq, preprocessorLifecycle);

    ResponseEntity<String> response = controller.startPreprocessorListener();

    verify(preprocessorLifecycle).resume();
    assertThat(response.getBody()).isEqualTo("Preprocessor listener started.");
  }

  @Test
  void stopsTheOwnedPreprocessorLifecycle() {
    DlqController controller = new DlqController(outcomeProcessPreprocessingDlq, preprocessorLifecycle);

    ResponseEntity<String> response = controller.stopPreprocessorListener();

    verify(preprocessorLifecycle).pause();
    assertThat(response.getBody()).isEqualTo("Preprocessor listener stopped.");
  }
}