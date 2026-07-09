package uk.gov.ons.census.fwmt.outcomeservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "feature-flags.outcome")
public class OutcomeFeatureFlagConfig {
  private Map<String, Boolean> surveys = new HashMap<>();

  public boolean isEnabledForSurvey(String survey) {
    if (survey == null) {
      return false;
    }

    return surveys.getOrDefault(normalizeSurvey(survey), false);
  }

  private String normalizeSurvey(String survey) {
    String normalized = survey.trim().toUpperCase();
    if ("CCS PL".equals(normalized) || "CCS INT".equals(normalized) || "CCS".equals(normalized)) {
      return "CCS";
    }
    return normalized;
  }
}