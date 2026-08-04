package uk.gov.ons.census.fwmt.outcomeservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomeFeatureFlagConfig;

/**
 * Runtime guard for survey outcome endpoints.
 * Returns 204 No Content when a survey feature flag is disabled,
 * without processing the outcome.
 */
@Slf4j
@Component
public class OutcomeFeatureFlagGuard {

  private final OutcomeFeatureFlagConfig featureFlagConfig;

  public OutcomeFeatureFlagGuard(OutcomeFeatureFlagConfig featureFlagConfig) {
    this.featureFlagConfig = featureFlagConfig;
  }

  /**
   * Check if a survey outcome endpoint should process a request.
   * @param survey survey name (e.g., "HH", "CE", "SPG")
   * @return true if survey is enabled, false otherwise
   */
  public boolean isEnabled(String survey) {
    return featureFlagConfig.isEnabledForSurvey(survey);
  }

  /**
   * Return a no-op response for disabled survey endpoints.
   * Called when feature flag for survey is disabled.
   * @param survey survey name for logging
   * @param endpoint endpoint name/path for logging
   * @return ResponseEntity with 204 No Content
   */
  public ResponseEntity<Void> handleDisabledSurvey(String survey, String endpoint) {
    log.info("Survey {} is disabled; skipping processing for endpoint {}", survey, endpoint);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}

