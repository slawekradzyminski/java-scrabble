package com.scrabble.backend;

import com.scrabble.dictionary.compile.DictionaryCompiler;
import com.scrabble.dictionary.format.DictionaryPaths;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DictionaryControllerTest {
  private static Path fstPath;
  private static Path metaPath;

  @LocalServerPort
  private int port;

  @BeforeAll
  static void setUpDictionary() throws Exception {
    Path tempDir = Files.createTempDirectory("backend-dictionary");
    fstPath = tempDir.resolve("osps.fst");
    metaPath = DictionaryPaths.metaPathFor(fstPath);

    Path input = loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("dictionary.fstPath", () -> fstPath.toString());
    registry.add("dictionary.metaPath", () -> metaPath.toString());
  }

  @Test
  void containsEndpointReturnsTrueForKnownWord() {
    WebTestClient client = buildClient();
    client.get()
        .uri(uriBuilder -> uriBuilder.path("/api/dictionary/contains")
            .queryParam("word", "zajawiałeś")
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.word").isEqualTo("zajawiałeś")
        .jsonPath("$.contains").isEqualTo(true);
  }

  @Test
  void containsEndpointReturnsFalseForUnknownWord() {
    WebTestClient client = buildClient();
    client.get()
        .uri(uriBuilder -> uriBuilder.path("/api/dictionary/contains")
            .queryParam("word", "nieistniejace")
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.contains").isEqualTo(false);
  }

  @Test
  void healthEndpointIsUp() {
    WebTestClient client = buildClient();
    client.get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("UP");
  }

  private static Path loadResourceToTempFile(String resourceName) throws IOException {
    try (InputStream input = DictionaryControllerTest.class.getResourceAsStream("/" + resourceName)) {
      if (input == null) {
        throw new IllegalStateException("Test resource not found: " + resourceName);
      }
      Path tempFile = Files.createTempFile("dictionary-resource-", "-" + resourceName);
      Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
      return tempFile;
    }
  }

  private WebTestClient buildClient() {
    return WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
  }
}
