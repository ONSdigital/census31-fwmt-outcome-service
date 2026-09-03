package uk.gov.ons.census.fwmt.outcomeservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DateConfigTest {

  @Test
  void dateFormat_formatsDatesInUtcRegardlessOfHostTimezone() {
    DateConfig dateConfig = new DateConfig();

    String formattedDate = dateConfig.dateFormat().format(Date.from(Instant.parse("2020-04-17T11:53:11Z")));

    assertThat(formattedDate).isEqualTo("2020-04-17T11:53:11.000Z");
  }
}