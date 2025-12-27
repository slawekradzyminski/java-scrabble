package com.scrabble.dictionary.format;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record DictionaryMeta(
    @JsonProperty("formatVersion") int formatVersion,
    @JsonProperty("normalisation") String normalisation,
    @JsonProperty("wordCount") long wordCount,
    @JsonProperty("sourceSha256") String sourceSha256,
    @JsonProperty("createdAt") Instant createdAt) {}
