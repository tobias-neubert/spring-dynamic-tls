package neubert.tobias.tls.messages;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MessageController {

  @GetMapping(value = "/message", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> saySomething() {
    return ResponseEntity.ok("pong");
  }
}
