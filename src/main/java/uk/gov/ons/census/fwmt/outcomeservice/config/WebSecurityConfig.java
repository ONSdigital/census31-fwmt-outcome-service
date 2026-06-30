package uk.gov.ons.census.fwmt.outcomeservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  private static final String[] AUTH_WHITELIST = {
      "/swagger-ui/**",
      "/v3/api-docs/**",
      "/actuator/health",
      "/info"
  };

  @Autowired
  private BasicAuthenticationPoint basicAuthenticationPoint;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(AUTH_WHITELIST).permitAll()
            .anyRequest().authenticated())
        .httpBasic(basic -> basic.authenticationEntryPoint(basicAuthenticationPoint));
    return http.build();
  }
}
