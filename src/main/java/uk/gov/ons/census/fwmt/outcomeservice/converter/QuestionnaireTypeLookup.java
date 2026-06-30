package uk.gov.ons.census.fwmt.outcomeservice.converter;

import java.util.HashMap;
import java.util.Map;

public class QuestionnaireTypeLookup {

  private final Map<String, String> questionnaireTypeToPackCode = new HashMap<>();

  public String getPackCode(String questionnaireType) {
    return questionnaireTypeToPackCode.get(questionnaireType);
  }

  public void add(String questionnaireType, String packCode) {
    questionnaireTypeToPackCode.put(questionnaireType, packCode);
  }
}
