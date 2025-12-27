package com.scrabble.dictionary.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DictionaryMetaIO {
  private static final ObjectMapper MAPPER = buildMapper();

  private DictionaryMetaIO() { }

  public static DictionaryMeta read(Path path) throws IOException {
    return MAPPER.readValue(Files.readAllBytes(path), DictionaryMeta.class);
  }

  public static void write(Path path, DictionaryMeta meta) throws IOException {
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }
    byte[] payload = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(meta);
    Files.write(path, payload);
  }

  private static ObjectMapper buildMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }
}
