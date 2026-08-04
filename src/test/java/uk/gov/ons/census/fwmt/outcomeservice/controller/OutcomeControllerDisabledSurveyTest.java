package uk.gov.ons.census.fwmt.outcomeservice.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.census.fwmt.common.data.household.HHOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.messaging.OutcomePreprocessingPublisher;
import uk.gov.ons.census.fwmt.outcomeservice.service.OutcomeFeatureFlagGuard;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for outcome controller behavior when survey flags are disabled.
 *
 * Verifies endpoints return 204 No Content without processing outcomes.
 */
@ExtendWith(MockitoExtension.class)
class OutcomeControllerDisabledSurveyTest {

  @Mock
  private GatewayEventManager gatewayEventManager;

  @Mock
  private OutcomePreprocessingPublisher outcomePreprocessingPublisher;

  @Mock
  private OutcomeFeatureFlagGuard featureFlagGuard;

  @InjectMocks
  private HhOutcomeController hhOutcomeController;

  @Test
  void testHhOutcomeReturnsNoContentWhenDisabled() throws GatewayException {
    // Setup: flag is disabled
    when(featureFlagGuard.isEnabled("HH")).thenReturn(false);
    when(featureFlagGuard.handleDisabledSurvey("HH", "/hhOutcome/{caseID}"))
        .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

    HHOutcome hhOutcome = new HHOutcome();
    hhOutcome.setTransactionId(UUID.randomUUID());

    // Execute
    ResponseEntity<Void> response = hhOutcomeController.hhOutcomeResponse("test-case-id", hhOutcome);

    // Verify returns 204
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertNull(response.getBody());
    
    // Verify no processing was done
    verify(gatewayEventManager, never()).triggerEvent(any(), any());
    verify(outcomePreprocessingPublisher, never()).sendHHOutcomeToPreprocessingQueue(any());
  }
}







