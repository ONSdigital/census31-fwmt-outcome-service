package uk.gov.ons.census.fwmt.outcomeservice.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestionnaireTypeLookupTest {

  private QuestionnaireTypeLookup questionnaireTypeLookup;

  @BeforeEach
  void setUp() throws Exception {
    questionnaireTypeLookup = new QuestionnaireTypeLookup();
    try (BufferedReader in = new BufferedReader(
        new InputStreamReader(new ClassPathResource("questionnaireTypeLookup.txt").getInputStream(), UTF_8))) {
      String line;
      while ((line = in.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        String[] lookup = line.split(",");
        questionnaireTypeLookup.add(lookup[0], lookup[1]);
      }
    }
  }

  @Test
  void mapsHuac1ToUachht1() {
    assertEquals("UACHHT1", questionnaireTypeLookup.getPackCode("HUAC1"));
  }

  @Test
  void mapsPaperHouseholdQuestionnaireTypes() {
    assertEquals("P_OR_H1", questionnaireTypeLookup.getPackCode("H1"));
    assertEquals("P_OR_HC2", questionnaireTypeLookup.getPackCode("HC2"));
    assertEquals("P_OR_I4", questionnaireTypeLookup.getPackCode("I4"));
  }

  @Test
  void returnsNullForUnknownQuestionnaireType() {
    assertNull(questionnaireTypeLookup.getPackCode("UNKNOWN"));
  }
}
