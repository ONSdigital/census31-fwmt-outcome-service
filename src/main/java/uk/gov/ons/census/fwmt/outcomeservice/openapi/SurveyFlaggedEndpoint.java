package uk.gov.ons.census.fwmt.outcomeservice.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller or method as a survey-flagged endpoint.
 * Used by OpenAPI customizer to filter/hide endpoints based on survey feature flags.
 *
 * Applied at class level to indicate all methods in controller belong to a survey.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SurveyFlaggedEndpoint {
  /**
   * @return survey identifier (e.g., "HH", "CE", "SPG", "CCS", "NC")
   */
  String value();
}

