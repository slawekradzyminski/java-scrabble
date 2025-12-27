package com.scrabble.backend.dictionary;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dictionary")
public class DictionaryProperties {
  private Path fstPath = Path.of("artifacts/osps.fst");
  private Path metaPath = Path.of("artifacts/osps.fst.meta.json");

  public Path getFstPath() {
    return fstPath;
  }

  public void setFstPath(Path fstPath) {
    this.fstPath = fstPath;
  }

  public Path getMetaPath() {
    return metaPath;
  }

  public void setMetaPath(Path metaPath) {
    this.metaPath = metaPath;
  }
}
