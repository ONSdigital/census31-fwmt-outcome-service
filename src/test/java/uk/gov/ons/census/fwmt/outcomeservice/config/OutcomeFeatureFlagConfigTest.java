package uk.gov.ons.census.fwmt.outcomeservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutcomeFeatureFlagConfigTest {

  @Test
  void resetAllFlags_enablesEverySupportedSurvey() {
    OutcomeFeatureFlagConfig config = new OutcomeFeatureFlagConfig();

    config.resetAllFlags(true);

    assertThat(config.isEnabledForSurvey("HH")).isTrue();
    assertThat(config.isEnabledForSurvey("SPG")).isTrue();
    assertThat(config.isEnabledForSurvey("CCS PL")).isTrue();
    assertThat(config.isEnabledForSurvey("NC")).isTrue();
  }

  @Test
  void setSurveyEnabled_overridesConfiguredValueForSurveyAlias() {
    OutcomeFeatureFlagConfig config = new OutcomeFeatureFlagConfig();
    Map<String, Boolean> configuredSurveys = new HashMap<>();
    configuredSurveys.put("CCS", true);
    config.setSurveys(configuredSurveys);

    config.setSurveyEnabled("CCS INT", false);

    assertThat(config.isEnabledForSurvey("CCS")).isFalse();
    assertThat(config.isEnabledForSurvey("CCS PL")).isFalse();
  }

  @Test
  void setSurveyEnabled_rejectsUnsupportedSurvey() {
    OutcomeFeatureFlagConfig config = new OutcomeFeatureFlagConfig();

    assertThatThrownBy(() -> config.setSurveyEnabled("XYZ", true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported survey");
  }

  @Test
  void isEnabledForSurvey_returnsFalseForBlankSurvey() {
    OutcomeFeatureFlagConfig config = new OutcomeFeatureFlagConfig();

    assertThat(config.isEnabledForSurvey(" ")).isFalse();
  }
}

