package uk.gov.ons.census.fwmt.outcomeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Configuration
public class DateConfig {
  @Bean
  DateFormat dateFormat() {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    return format;
  }
}
