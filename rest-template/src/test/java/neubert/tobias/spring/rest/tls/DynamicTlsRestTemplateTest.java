package neubert.tobias.spring.rest.tls;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static neubert.tobias.spring.rest.tls.TlsExtension.mockServer;
import static neubert.tobias.spring.rest.tls.TlsExtension.mockServerPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


/**
 * Generate the certificate for this test as follows:
 *
 * <pre>
 * openssl req -x509 -sha256 -nodes -days 36500 -newkey rsa:4096 -keyout private.key -out certificate.pem
 * </pre>
 */
@SpringBootTest(classes = {DynamicTlsConfig.class})
@ExtendWith(TlsExtension.class)
class DynamicTlsRestTemplateTest {
  @Autowired
  private DynamicTlsRestTemplate restTemplate;

  @Test
  void switchingTruststores() throws Exception {
    mockServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ping")
      )
      .respond(
        response()
          .withStatusCode(200)
          .withBody("pong")
      );

    ResponseEntity<String> pong = restTemplate.getForEntity(format("https://localhost:%s/ping", mockServerPort), String.class);
    assertThat(pong.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(pong.getBody()).isEqualTo("pong");

    // change the truststore at runtime, wait a little for the rest template to refresh the store
    Files.copy(TlsExtension.flyingDesksTrustPath, TlsExtension.trustPath, REPLACE_EXISTING);
    Thread.sleep(1000);

    assertThatThrownBy(() -> restTemplate.getForEntity(format("https://localhost:%s/ping", mockServerPort), String.class))
      .hasMessageContaining("PKIX path building failed");

    // change the truststore at runtime, wait a little for the rest template to refresh the store
    Files.copy(TlsExtension.tobiasTrustPath, TlsExtension.trustPath, REPLACE_EXISTING);
    Thread.sleep(1000);

    pong = restTemplate.getForEntity(format("https://localhost:%s/ping", mockServerPort), String.class);
    assertThat(pong.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(pong.getBody()).isEqualTo("pong");
  }
}