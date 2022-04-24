package neubert.tobias.jetty.tls;

import org.springframework.core.io.Resource;


public record TlsJettyProperties(
  Resource identityCertResource,
  Resource identityKeyResource,
  Resource trustResource)
{
}
