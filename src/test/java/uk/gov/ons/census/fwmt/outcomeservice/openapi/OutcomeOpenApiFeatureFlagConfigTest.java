package uk.gov.ons.census.fwmt.outcomeservice.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomeFeatureFlagConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutcomeOpenApiFeatureFlagConfigTest {

  @Mock
  private OutcomeFeatureFlagConfig featureFlagConfig;

  private OutcomeOpenApiFeatureFlagConfig customizer;

  @BeforeEach
  void setUp() {
    customizer = new OutcomeOpenApiFeatureFlagConfig(featureFlagConfig);
  }

  @Test
  void testCustomiseRemovesDisabledSurveyPaths() {
    // All surveys enabled
    when(featureFlagConfig.isEnabledForSurvey("HH")).thenReturn(true);
    when(featureFlagConfig.isEnabledForSurvey("CE")).thenReturn(true);
    when(featureFlagConfig.isEnabledForSurvey("SPG")).thenReturn(false);
    when(featureFlagConfig.isEnabledForSurvey("CCS")).thenReturn(true);
    when(featureFlagConfig.isEnabledForSurvey("NC")).thenReturn(true);

    OpenAPI openAPI = new OpenAPI();
    Paths paths = new Paths();

    // Add a path for HH (should remain)
    PathItem hhPath = new PathItem();
    Operation hhOp = new Operation();
    hhOp.addTagsItem("HH Outcomes");
    hhPath.post(hhOp);
    paths.addPathItem("/hhOutcome/{caseID}", hhPath);

    // Add a path for SPG (should be removed because SPG is disabled)
    PathItem spgPath = new PathItem();
    Operation spgOp = new Operation();
    spgOp.addTagsItem("SPG Outcomes");
    spgPath.post(spgOp);
    paths.addPathItem("/spgOutcome/{caseID}", spgPath);

    openAPI.setPaths(paths);

    // Apply customization
    customizer.customise(openAPI);

    // Verify: HH path should remain, SPG path should be removed
    assertTrue(openAPI.getPaths().containsKey("/hhOutcome/{caseID}"));
    assertFalse(openAPI.getPaths().containsKey("/spgOutcome/{caseID}"));
  }

  @Test
  void testCustomiseHandlesEmptyPaths() {
    OpenAPI openAPI = new OpenAPI();
    Paths paths = new Paths();
    openAPI.setPaths(paths);

    // Should not throw exception with empty paths
    assertDoesNotThrow(() -> customizer.customise(openAPI));
  }

  @Test
  void testCustomiseHandlesNullPaths() {
    OpenAPI openAPI = new OpenAPI();
    openAPI.setPaths(null);

    // Should not throw exception with null paths
    assertDoesNotThrow(() -> customizer.customise(openAPI));
  }

  @Test
  void testCustomiseAllSurveysEnabled() {
    when(featureFlagConfig.isEnabledForSurvey("HH")).thenReturn(true);
    when(featureFlagConfig.isEnabledForSurvey("CE")).thenReturn(true);
    when(featureFlagConfig.isEnabledForSurvey("SPG")).thenReturn(true);
    when(featureFlagConfig.isEnabledForSurvey("CCS")).thenReturn(true);
    when(featureFlagConfig.isEnabledForSurvey("NC")).thenReturn(true);

    OpenAPI openAPI = new OpenAPI();
    Paths paths = new Paths();

    PathItem hhPath = new PathItem();
    Operation hhOp = new Operation();
    hhOp.addTagsItem("HH Outcomes");
    hhPath.post(hhOp);
    paths.addPathItem("/hhOutcome/{caseID}", hhPath);

    openAPI.setPaths(paths);

    // Apply customization
    customizer.customise(openAPI);

    // All paths should remain since all surveys are enabled
    assertTrue(openAPI.getPaths().containsKey("/hhOutcome/{caseID}"));
  }
}

