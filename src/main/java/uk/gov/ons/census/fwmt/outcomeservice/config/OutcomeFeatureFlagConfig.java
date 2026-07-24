package uk.gov.ons.census.fwmt.outcomeservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "feature-flags.outcome")
public class OutcomeFeatureFlagConfig {
  private static final Set<String> SUPPORTED_SURVEYS = buildSupportedSurveys();

  private Map<String, Boolean> surveys = new HashMap<>();
  private final Map<String, Boolean> runtimeOverrides = new ConcurrentHashMap<>();

  public boolean isEnabledForSurvey(String survey) {
    String normalizedSurvey = normalizeSurvey(survey);
    if (normalizedSurvey == null) {
      return false;
    }

    Boolean runtimeOverride = runtimeOverrides.get(normalizedSurvey);
    if (runtimeOverride != null) {
      return runtimeOverride;
    }

    return surveys.getOrDefault(normalizedSurvey, false);
  }

  public void resetAllFlags(boolean enabled) {
    runtimeOverrides.clear();
    SUPPORTED_SURVEYS.forEach(survey -> runtimeOverrides.put(survey, enabled));
  }

  public void setSurveyEnabled(String survey, boolean enabled) {
    String normalizedSurvey = normalizeSurvey(survey);
    if (normalizedSurvey == null || !SUPPORTED_SURVEYS.contains(normalizedSurvey)) {
      throw new IllegalArgumentException("Unsupported survey '" + survey + "'");
    }
    runtimeOverrides.put(normalizedSurvey, enabled);
  }

  private String normalizeSurvey(String survey) {
    if (survey == null) {
      return null;
    }

    String normalized = survey.trim().toUpperCase();
    if (normalized.isEmpty()) {
      return null;
    }

    if ("CCS PL".equals(normalized) || "CCS INT".equals(normalized) || "CCS".equals(normalized)) {
      return "CCS";
    }
    return normalized;
  }

  private static Set<String> buildSupportedSurveys() {
    Set<String> supportedSurveys = new LinkedHashSet<>();
    Collections.addAll(supportedSurveys, "HH", "SPG", "CE", "CCS", "NC");
    return Collections.unmodifiableSet(supportedSurveys);
  }
}