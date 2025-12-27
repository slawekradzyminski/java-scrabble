package com.scrabble.backend;

import com.scrabble.dictionary.compile.DictionaryCompiler;
import com.scrabble.dictionary.format.DictionaryPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    Path input = findRepoFile("osps_shortened.txt");
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

  private static Path findRepoFile(String filename) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(filename);
      if (candidate.toFile().exists()) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Unable to locate " + filename + " from " + Path.of("").toAbsolutePath());
  }

  private WebTestClient buildClient() {
    return WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
  }
}
