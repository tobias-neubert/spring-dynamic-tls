package neubert.tobias.jetty.tls;

import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.JettySslUtils;
import nl.altindag.ssl.util.PemUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Collections;


@Configuration
@ComponentScan
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class SecureJettyConfig {
  @Bean
  public TlsJettyProperties tlsJettyProperties(
    @Value("${neubert.tobias.tls.identity-cert-resource}") Resource identityCertResource,
    @Value("${neubert.tobias.tls.identity-key-resource}") Resource identityKeyResource,
    @Value("${neubert.tobias.tls.trust-resource}") Resource trustResource)
  {
    return new TlsJettyProperties(identityCertResource, identityKeyResource, trustResource);
  }

  @Bean
  public JettyServletWebServerFactory JettyServletWebServerFactory(
    SslContextFactory.Server sslContextFactory,
    ServerProperties serverProperties)
  {
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory();

    JettyServerCustomizer jettyServerCustomizer = server -> {
      ServerConnector serverConnector = new ServerConnector(server, sslContextFactory);
      serverConnector.setPort(serverProperties.getPort());
      server.setConnectors(new Connector[]{serverConnector});
    };
    factory.setServerCustomizers(Collections.singletonList(jettyServerCustomizer));

    return factory;
  }

  @Bean
  public X509ExtendedKeyManager keyManager(SSLFactory sslFactory) {
    return sslFactory.getKeyManager().orElseThrow();
  }

  @Bean
  public X509ExtendedTrustManager trustManager(SSLFactory sslFactory) {
    return sslFactory.getTrustManager().orElseThrow();
  }

  @Bean
  public SSLSessionContext serverSessionContext(SSLFactory sslFactory) {
    return sslFactory.getSslContext().getServerSessionContext();
  }

  @Bean
  public SSLFactory sslFactory(TlsJettyProperties tlsProperties) throws IOException {
    X509ExtendedKeyManager keyManager =
      PemUtils.loadIdentityMaterial(
        tlsProperties.identityCertResource().getInputStream(),
        tlsProperties.identityKeyResource().getInputStream());
    X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(tlsProperties.trustResource().getInputStream());

    return SSLFactory.builder()
      .withSwappableIdentityMaterial()
      .withIdentityMaterial(keyManager)
      .withSwappableTrustMaterial()
      .withTrustMaterial(trustManager)
      .withNeedClientAuthentication(false)
      .build();
  }

  @Bean
  public SslContextFactory.Server sslContextFactory(SSLFactory sslFactory) {
    return JettySslUtils.forServer(sslFactory);
  }
}
