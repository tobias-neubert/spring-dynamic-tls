package neubert.tobias.tls.hello;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;


@RestController
public class HelloWorldController {
  private final String messageUrl;
  private final RestTemplate restTemplate;

  public HelloWorldController(
    @Value("${neubert.tobias.hello.message-url}") String messageUrl,
    RestTemplate restTemplate)
  {
    this.messageUrl = messageUrl;
    this.restTemplate = restTemplate;
  }

  @GetMapping(value = "/hello/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> hello(@PathVariable(name="name") String name) {
    ResponseEntity<String> message = restTemplate.getForEntity(messageUrl, String.class);

    if (message.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException(format("Message service returned %d", message.getStatusCode().value()));
    }

    return ResponseEntity.ok("hello " + name + ", " + message.getBody());
  }
}
