package neubert.tobias.spring.cloud.config.client.tls;

import neubert.tobias.spring.rest.tls.DynamicTlsRestTemplate;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;


@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DynamicTlsConfigClientBootstrapConfiguration {
  @Bean
  public ConfigServicePropertySourceLocator configServicePropertySourceLocator(
    ConfigClientProperties clientProperties,
    DynamicTlsRestTemplate restTemplate)
  {
    ConfigServicePropertySourceLocator configServicePropertySourceLocator = new ConfigServicePropertySourceLocator(clientProperties);
    configServicePropertySourceLocator.setRestTemplate(restTemplate);
    return configServicePropertySourceLocator;
  }
}