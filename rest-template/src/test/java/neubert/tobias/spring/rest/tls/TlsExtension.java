package neubert.tobias.spring.rest.tls;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;


public class TlsExtension implements BeforeAllCallback, AfterAllCallback {
  private Path tlsPath;

  static Path flyingDesksTrustPath;
  static Path tobiasTrustPath;
  static Path trustPath;
  static ClientAndServer mockServer;
  static int mockServerPort;


  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    tlsPath = Files.createTempDirectory("tls");
    trustPath = tlsPath.resolve("certificate.pem");
    tobiasTrustPath = tlsPath.resolve("tobias-certificate.pem");
    flyingDesksTrustPath = tlsPath.resolve("flying-desks-certificate.pem");

    Files.copy(getClass().getResourceAsStream("/tls/tobias-certificate.pem"), tobiasTrustPath);
    Files.copy(getClass().getResourceAsStream("/tls/tobias-certificate.pem"), trustPath);
    Files.copy(getClass().getResourceAsStream("/tls/flying-desks-certificate.pem"), flyingDesksTrustPath);

    System.setProperty(
      "neubert.tobias.tls.trust-resource",
      format("file:%s", trustPath.toFile().getAbsolutePath())
    );

    ConfigurationProperties.certificateAuthorityPrivateKey(getClass().getResource("/tls/private.key").getFile());
    ConfigurationProperties.certificateAuthorityCertificate(getClass().getResource("/tls/tobias-certificate.pem").getFile());

    mockServerPort = PortFactory.findFreePort();
    mockServer = ClientAndServer.startClientAndServer(mockServerPort);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Files.deleteIfExists(trustPath);
    Files.deleteIfExists(tobiasTrustPath);
    Files.deleteIfExists(flyingDesksTrustPath);
    Files.deleteIfExists(tlsPath);

    mockServer.stop();
  }
}
