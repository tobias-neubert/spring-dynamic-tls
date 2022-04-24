package neubert.tobias.spring.rest.tls;

import javax.net.ssl.X509ExtendedTrustManager;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.PemUtils;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


public class DynamicTlsRestTemplate extends RestTemplate {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTlsRestTemplate.class);

  private final Resource trustResource;
  private final Path trustPath;

  public DynamicTlsRestTemplate(Resource trustResource) throws IOException {
    this.trustResource = trustResource;
    this.trustPath = Path.of(trustResource.getFile().getAbsolutePath());

    setRequestFactory(httpRequestFactory());
    Executors.newSingleThreadExecutor().submit(this::startTrustWatcher);
  }

  void startTrustWatcher() {
    try {
      WatchService fileWatcher = FileSystems.getDefault().newWatchService();
      trustPath.getParent().register(fileWatcher, ENTRY_MODIFY);

      while (true) {
        WatchKey watchKey = fileWatcher.take();
        for (WatchEvent<?> fileEvent : watchKey.pollEvents()) {
          LOGGER.info("Recognised modification of {}", fileEvent.context());
          Path modifiedPath = (Path) fileEvent.context();
          if (trustPath.endsWith(modifiedPath)) {
            updateTlsMaterial();
          }
        }

        watchKey.reset();
        LOGGER.info("Finished working on modifications of {}", trustPath);
      }
    }
    catch (Exception e) {
      LOGGER.error("Cannot start the trust watcher", e);
    }
  }

  private void updateTlsMaterial() {
    try {
      LOGGER.info("Trust certificate {} has been changed for the RestTemplate. ", trustPath);

      setRequestFactory(httpRequestFactory());

      LOGGER.info("Updating trust {} for the RestTemplate finished", trustPath);
    }
    catch (Exception e) {
      LOGGER.error("" +
        "Cannot update the TLS material for the RestTemplate. Proceed watching for changes. " +
        "This might be no problem at all because some file systems fire a modification event " +
        "twice, where one is for an incomplete file. If the certificate is not changed indeed " +
        "you have to look into the log at debug level.");
      LOGGER.debug("", e);
    }
  }

  private ClientHttpRequestFactory httpRequestFactory() throws IOException {
    final CloseableHttpClient httpClient =
      HttpClients.custom()
        .setSSLSocketFactory(sslSocketFactory())
        .build();

    final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    requestFactory.setHttpClient(httpClient);
    requestFactory.setConnectTimeout(5_000);
    requestFactory.setReadTimeout(30_000);

    return requestFactory;
  }

  private LayeredConnectionSocketFactory sslSocketFactory() throws IOException {
    return new SSLConnectionSocketFactory(sslFactory().getSslContext());
  }

  private SSLFactory sslFactory() throws IOException {
    X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(trustResource.getInputStream());

    return SSLFactory.builder()
      .withSwappableTrustMaterial()
      .withTrustMaterial(trustManager)
      .withNeedClientAuthentication(false)
      .build();
  }
}
