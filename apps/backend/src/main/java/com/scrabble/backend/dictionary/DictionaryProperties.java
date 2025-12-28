package com.scrabble.backend.dictionary;

import java.nio.file.Path;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dictionary")
@Data
public class DictionaryProperties {
  private Path fstPath = Path.of("artifacts/osps.fst");
  private Path metaPath = Path.of("artifacts/osps.fst.meta.json");
}
