package uk.gov.ons.census.fwmt.outcomeservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomeFeatureFlagConfig;

class OutcomeFeatureFlagTestSupportControllerTest {

  private final OutcomeFeatureFlagConfig featureFlagConfig = new OutcomeFeatureFlagConfig();
  private final OutcomeFeatureFlagTestSupportController controller =
      new OutcomeFeatureFlagTestSupportController(featureFlagConfig);

  @Test
  void resetAllFeatureFlags_updatesAllSurveys() {
    ResponseEntity<Void> response = controller.resetAllFeatureFlags(
        new OutcomeFeatureFlagTestSupportController.ResetFeatureFlagsRequest(true));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(featureFlagConfig.isEnabledForSurvey("HH")).isTrue();
    assertThat(featureFlagConfig.isEnabledForSurvey("CCS INT")).isTrue();
    assertThat(featureFlagConfig.isEnabledForSurvey("NC")).isTrue();
  }

  @Test
  void setOutcomeFeatureFlag_updatesOnlyRequestedSurvey() {
    controller.resetAllFeatureFlags(new OutcomeFeatureFlagTestSupportController.ResetFeatureFlagsRequest(true));

    ResponseEntity<Void> response = controller.setOutcomeFeatureFlag(
        new OutcomeFeatureFlagTestSupportController.OutcomeFeatureFlagRequest("HH", false));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(featureFlagConfig.isEnabledForSurvey("HH")).isFalse();
    assertThat(featureFlagConfig.isEnabledForSurvey("SPG")).isTrue();
  }

  @Test
  void resetAllFeatureFlags_rejectsMissingEnabledValue() {
    assertThatThrownBy(() -> controller.resetAllFeatureFlags(
        new OutcomeFeatureFlagTestSupportController.ResetFeatureFlagsRequest(null)))
        .isInstanceOfSatisfying(ResponseStatusException.class,
            error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }
}

