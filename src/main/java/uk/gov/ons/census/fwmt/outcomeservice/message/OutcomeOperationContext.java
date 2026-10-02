package uk.gov.ons.census.fwmt.outcomeservice.message;

public final class OutcomeOperationContext {

  private static final ThreadLocal<Details> CURRENT = new ThreadLocal<>();

  private OutcomeOperationContext() {
  }

  public static void set(Details details) {
    CURRENT.set(details);
  }

  public static Details get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }

  public record Details(
      String operation,
      String outcomeCode,
      String transactionId,
      String caseId,
      String surveyType) {
  }
}
