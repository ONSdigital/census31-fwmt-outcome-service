package uk.gov.ons.census.fwmt.outcomeservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    String serverUrl = getServerUrl();
    return new OpenAPI()
        .openapi("3.2.0")
        .info(new Info()
            .title("FWMT Gateway - Outcome Service")
            .version("1.0.0"))
        .addServersItem(new Server()
            .url(serverUrl)
            .description("Default Server"));
  }

  private String getServerUrl() {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        return ServletUriComponentsBuilder.fromRequestUri(attributes.getRequest())
            .replacePath("")
            .build()
            .toUriString();
      }
    } catch (Exception e) {
      // Fall back if no request context available
    }
    return "/";
  }
}
