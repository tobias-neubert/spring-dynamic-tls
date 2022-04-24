package neubert.tobias.jetty.tls;

import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.KeyManagerUtils;
import nl.altindag.ssl.util.PemUtils;
import nl.altindag.ssl.util.SSLSessionUtils;
import nl.altindag.ssl.util.TrustManagerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


@Service
public class FileBasedTlsUpdateService implements DisposableBean {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedTlsUpdateService.class);

  private final SSLSessionContext sslSessionContext;
  private final X509ExtendedKeyManager identityManager;
  private final X509ExtendedTrustManager trustManager;
  private final TlsJettyProperties tlsProperties;
  private final Path identityCertPath;
  private final Path identityKeyPath;
  private final Path trustCertPath;

  private WatchService fileWatcher;
  private boolean watching = true;

  public FileBasedTlsUpdateService(
    SSLSessionContext sslSessionContext,
    X509ExtendedKeyManager identityManager,
    X509ExtendedTrustManager trustManager,
    TlsJettyProperties tlsProperties) throws IOException
  {
    this.sslSessionContext = sslSessionContext;
    this.identityManager = identityManager;
    this.trustManager = trustManager;
    this.tlsProperties = tlsProperties;
    this.identityCertPath = Path.of(tlsProperties.identityCertResource().getFile().getAbsolutePath());
    this.identityKeyPath = Path.of(tlsProperties.identityKeyResource().getFile().getAbsolutePath());
    this.trustCertPath = Path.of(tlsProperties.trustResource().getFile().getAbsolutePath());

    Executors.newSingleThreadExecutor().submit(this::startFileWatcher);
  }

  public void stop() {
    try {
      if (watching) {
        LOGGER.info("Stopping the file watcher");

        watching = false;
        fileWatcher.close();
      }
    }
    catch (IOException e) {
      LOGGER.warn("Error while closing the file watcher.", e);
    }
  }

  private void startFileWatcher() {
    try {
      fileWatcher = FileSystems.getDefault().newWatchService();
      trustCertPath.getParent().register(fileWatcher, ENTRY_MODIFY);
      identityCertPath.getParent().register(fileWatcher, ENTRY_MODIFY);

      while (watching) {
        WatchKey watchKey = fileWatcher.take();
        for (WatchEvent<?> fileEvent : watchKey.pollEvents()) {
          LOGGER.info("Recognised modification of {}", fileEvent.context());
          Path modifiedPath = (Path) fileEvent.context();
          if (trustCertPath.endsWith(modifiedPath) || identityCertPath.endsWith(modifiedPath)) {
            updateTlsMaterial();
          }
        }

        watchKey.reset();
      }
    }
    catch (Exception e) {
      LOGGER.error("Cannot start the trust watcher", e);
    }
  }

  private void updateTlsMaterial() {
    try {
      LOGGER.info("Going to update the TLS material for the Jetty server");

      X509ExtendedKeyManager newIdentityManager =
        PemUtils.loadIdentityMaterial(
          tlsProperties.identityCertResource().getInputStream(),
          tlsProperties.identityKeyResource().getInputStream()
        );
      X509ExtendedTrustManager newTrustManager =
        PemUtils.loadTrustMaterial(
          tlsProperties.trustResource().getInputStream());

      SSLFactory sslFactory =
        SSLFactory.builder()
          .withIdentityMaterial(newIdentityManager)
          .withTrustMaterial(newTrustManager)
          .build();

      KeyManagerUtils.swapKeyManager(identityManager, sslFactory.getKeyManager().orElseThrow());
      TrustManagerUtils.swapTrustManager(trustManager, sslFactory.getTrustManager().orElseThrow());
      SSLSessionUtils.invalidateCaches(sslSessionContext);

      LOGGER.info("Updating TLS material finished for the Jetty server");
    }
    catch (Exception e) {
      LOGGER.error("" +
        "Cannot update the TLS material for the server. Proceed watching for changes. " +
        "This might be no problem at all because some file systems fire a modification event " +
        "twice, where one is for an incomplete file. If the certificate is not changed indeed " +
        "you have to look into the log at debug level.");
      LOGGER.debug("", e);
    }
  }

  @Override
  public void destroy() throws Exception {
    stop();
  }
}
