package uk.gov.ons.census.fwmt.outcomeservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomeFeatureFlagConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutcomeFeatureFlagGuardTest {

  @Mock
  private OutcomeFeatureFlagConfig featureFlagConfig;

  @InjectMocks
  private OutcomeFeatureFlagGuard guard;

  @BeforeEach
  void setUp() {
  }

  @Test
  void testIsEnabledWhenSurveyFlagTrue() {
    when(featureFlagConfig.isEnabledForSurvey("HH")).thenReturn(true);
    assertTrue(guard.isEnabled("HH"));
  }

  @Test
  void testIsEnabledWhenSurveyFlagFalse() {
    when(featureFlagConfig.isEnabledForSurvey("SPG")).thenReturn(false);
    assertFalse(guard.isEnabled("SPG"));
  }

  @Test
  void testHandleDisabledSurveyReturnsNoContent() {
    ResponseEntity<Void> response = guard.handleDisabledSurvey("CE", "/ceOutcome");
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void testHandleDisabledSurveyLogsCorrectly() {
    // Verify that no exceptions are thrown
    ResponseEntity<Void> response = guard.handleDisabledSurvey("CCS", "/ccsPropertyListingOutcome");
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }
}

