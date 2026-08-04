package uk.gov.ons.census.fwmt.outcomeservice.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomeFeatureFlagConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Springdoc OpenAPI customizer that dynamically hides endpoints for disabled surveys.
 *
 * This customizer is called each time OpenAPI docs are generated.
 * When a survey feature flag is disabled, all paths tagged with that survey are removed from the docs.
 *
 * For dynamic visibility to work, Springdoc cache must be disabled
 * (see application.yml springdoc.cache.disabled: true).
 */
@Slf4j
@Component
public class OutcomeOpenApiFeatureFlagConfig implements OpenApiCustomizer {

  private static final String HH_TAG = "HH Outcomes";
  private static final String CE_TAG = "CE Outcomes";
  private static final String SPG_TAG = "SPG Outcomes";
  private static final String CCS_TAG = "CCS Outcomes";
  private static final String NC_TAG = "NC Outcomes";

  private final OutcomeFeatureFlagConfig featureFlagConfig;

  public OutcomeOpenApiFeatureFlagConfig(OutcomeFeatureFlagConfig featureFlagConfig) {
    this.featureFlagConfig = featureFlagConfig;
  }

  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
      return;
    }

    // Check each enabled survey and collect tags to hide
    List<String> tagsToHide = new ArrayList<>();
    if (!featureFlagConfig.isEnabledForSurvey("HH")) {
      tagsToHide.add(HH_TAG);
    }
    if (!featureFlagConfig.isEnabledForSurvey("CE")) {
      tagsToHide.add(CE_TAG);
    }
    if (!featureFlagConfig.isEnabledForSurvey("SPG")) {
      tagsToHide.add(SPG_TAG);
    }
    if (!featureFlagConfig.isEnabledForSurvey("CCS")) {
      tagsToHide.add(CCS_TAG);
    }
    if (!featureFlagConfig.isEnabledForSurvey("NC")) {
      tagsToHide.add(NC_TAG);
    }
      if (openApi.getTags() != null) {
          openApi.getTags().removeIf(tag -> tagsToHide.contains(tag.getName()));
      }

    // Remove all paths that only belong to disabled-survey tags
    openApi.getPaths().values().removeIf(pathItem -> {
      if (pathItem.readOperations() == null || pathItem.readOperations().isEmpty()) {
        return false;
      }

      // Remove the operation if all its tags are in the disabled list
      pathItem.readOperations().forEach(operation -> {
        if (operation.getTags() != null) {
          operation.getTags().removeAll(tagsToHide);
        }
      });

      // If operation has no tags left after filtering, the path is essentially hidden
      return pathItem.readOperations().stream()
          .allMatch(op -> op.getTags() == null || op.getTags().isEmpty());
    });

    if (!tagsToHide.isEmpty()) {
      log.debug("OpenAPI customizer removed paths for disabled surveys: {}", tagsToHide);
    }
  }
}


