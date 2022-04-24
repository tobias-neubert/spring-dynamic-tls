package neubert.tobias.jetty.tls;

import nl.altindag.ssl.util.CertificateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * The TLS material was created like this:
 * <pre>
 * openssl req -x509 -sha256 -nodes -days 36500 -newkey rsa:4096 -keyout private.key -out certificate.pem
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({TlsExtension.class})
class FileBasedTlsUpdateServiceTest {
  @LocalServerPort
  int serverPort;

  private String pingUrl;

  @Autowired
  private FileBasedTlsUpdateService fileBasedTlsUpdateService;

  @BeforeEach
  void setup() {
    pingUrl = format("https://localhost:%d/ping", serverPort);
  }

  @Test
  void test() throws Exception {
    assertServerCertificate(pingUrl, "tobi@s-neubert.net, CN=localhost, OU=Development, O=Tobias Neubert");

    Files.copy(TlsExtension.flyingDesksIdentityKeyPath, TlsExtension.identityKeyPath, REPLACE_EXISTING);
    Files.copy(TlsExtension.flyingDesksIdentityCertPath, TlsExtension.identityCertPath, REPLACE_EXISTING);
    Thread.sleep(1000);

    assertServerCertificate(pingUrl, "info@flying-desks.com, CN=localhost, OU=Development, O=Flying Desks GmbH");

    Files.copy(TlsExtension.tobiasIdentityCertPath, TlsExtension.identityCertPath, REPLACE_EXISTING);
    Files.copy(TlsExtension.tobiasIdentityKeyPath, TlsExtension.identityKeyPath, REPLACE_EXISTING);
    Thread.sleep(1000);

    assertServerCertificate(pingUrl, "tobi@s-neubert.net, CN=localhost, OU=Development, O=Tobias Neubert");

    // important to release the tls files in order to allow the TlsExtension to clean up the temp folder
    fileBasedTlsUpdateService.stop();
  }

  private void assertServerCertificate(String httpsUrl, String issuerPattern) {
    Map<String, List<X509Certificate>> certificates = CertificateUtils.getCertificate(httpsUrl);
    assertThat(certificates).hasSize(1);
    assertThat(certificates.get(httpsUrl)).hasSize(1);

    X509Certificate certificate = certificates.get(httpsUrl).get(0);
    assertThat(certificate.getIssuerX500Principal().getName("RFC1779")).contains(issuerPattern);
  }
}