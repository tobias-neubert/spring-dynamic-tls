package neubert.tobias.spring.rest.tls;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;

import java.io.IOException;


@Configuration
public class DynamicTlsConfig {
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public DynamicTlsRestTemplate dynamicTlsRestTemplate(
    @Value("${neubert.tobias.tls.trust-resource}") Resource trustResource) throws IOException
  {
    return new DynamicTlsRestTemplate(trustResource);
  }
}
