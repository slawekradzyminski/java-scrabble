package com.scrabble.backend.ws;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsMessage {
  private WsMessageType type;
  private Map<String, Object> payload;
}
