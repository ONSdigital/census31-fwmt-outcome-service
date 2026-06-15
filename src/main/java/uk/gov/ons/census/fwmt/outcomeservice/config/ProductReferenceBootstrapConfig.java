package uk.gov.ons.census.fwmt.outcomeservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import uk.gov.ons.ctp.integration.common.product.ProductReference;

@Configuration
public class ProductReferenceBootstrapConfig {

  @Autowired
  private ProductReference productReference;

  @PostConstruct
  public void initProductReference() throws Exception {
    productReference.initProducts();
  }
}
