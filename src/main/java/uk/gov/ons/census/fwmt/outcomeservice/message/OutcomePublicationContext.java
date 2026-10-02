package uk.gov.ons.census.fwmt.outcomeservice.message;

import java.util.Date;
import java.util.UUID;

public record OutcomePublicationContext(
    String caseId,
    UUID transactionId,
    Date eventDate,
    String correlationId) {
}