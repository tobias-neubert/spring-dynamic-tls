package neubert.tobias.jetty.tls;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;


/**
 * keystores created like so:
 *
 * <pre>
 * openssl req -x509 -sha256 -nodes -days 36500 -newkey rsa:4096 -keyout private.key -out certificate.pem
 * openssl pkcs12 -export -out certificate.pkcs12 -in certificate.pem -inkey private.key
 *
 * # the following command creates a dummy keystore that will be deleted afterwards in order to
 * # have an empty truststore
 * keytool -genkey -keyalg RSA -alias server -keystore trust.jks
 * keytool -delete -alias server -keystore trust.jks
 * keytool -import -v -trustcacerts -alias server -file certificate.pem -keystore trust.jks
 *
 * # and now the same for the keystore
 * keytool -genkey -keyalg RSA -alias server -keystore identity.jks
 * keytool -delete -alias server -keystore identity.jks
 * keytool -v -importkeystore -srckeystore certificate.pkcs12 -srcstoretype PKCS12 -destkeystore identity.jks -deststoretype JKS
 * </pre>
 */
public class TlsExtension implements BeforeAllCallback, AfterAllCallback {
  private Path tlsPath;

  static Path flyingDesksTrustPath;
  static Path tobiasTrustPath;
  static Path trustPath;

  static Path flyingDesksIdentityCertPath;
  static Path flyingDesksIdentityKeyPath;
  static Path tobiasIdentityCertPath;
  static Path tobiasIdentityKeyPath;
  static Path identityCertPath;
  static Path identityKeyPath;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    tlsPath = Files.createTempDirectory("tls");

    trustPath = tlsPath.resolve("certificate.pem");
    tobiasTrustPath = tlsPath.resolve("tobias-certificate.pem");
    flyingDesksTrustPath = tlsPath.resolve("flying-desks-certificate.pem");
    Files.copy(getClass().getResourceAsStream("/tls/tobias-certificate.pem"), tobiasTrustPath);
    Files.copy(getClass().getResourceAsStream("/tls/tobias-certificate.pem"), trustPath);
    Files.copy(getClass().getResourceAsStream("/tls/flying-desks-certificate.pem"), flyingDesksTrustPath);

    identityCertPath = tlsPath.resolve("certificate.pem");
    identityKeyPath = tlsPath.resolve("private.key");
    tobiasIdentityCertPath = tlsPath.resolve("tobias-certificate.pem");
    tobiasIdentityKeyPath = tlsPath.resolve("tobias-private.key");
    flyingDesksIdentityCertPath = tlsPath.resolve("flying-desks-certificate.pem");
    flyingDesksIdentityKeyPath = tlsPath.resolve("flying-desks-private.key");
    Files.copy(getClass().getResourceAsStream("/tls/tobias-private.key"), tobiasIdentityKeyPath);
    Files.copy(getClass().getResourceAsStream("/tls/tobias-private.key"), identityKeyPath);
    Files.copy(getClass().getResourceAsStream("/tls/flying-desks-private.key"), flyingDesksIdentityKeyPath);

    System.setProperty(
      "neubert.tobias.tls.trust-resource",
      format("file:%s", trustPath.toFile().getAbsolutePath())
    );
    System.setProperty(
      "neubert.tobias.tls.identity-cert-resource",
      format("file:%s", identityCertPath.toFile().getAbsolutePath())
    );
    System.setProperty(
      "neubert.tobias.tls.identity-key-resource",
      format("file:%s", identityKeyPath.toFile().getAbsolutePath())
    );
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Files.deleteIfExists(trustPath);
    Files.deleteIfExists(tobiasTrustPath);
    Files.deleteIfExists(flyingDesksTrustPath);
    Files.deleteIfExists(identityKeyPath);
    Files.deleteIfExists(tobiasIdentityKeyPath);
    Files.deleteIfExists(flyingDesksIdentityKeyPath);
    Files.deleteIfExists(tlsPath);
  }
}
