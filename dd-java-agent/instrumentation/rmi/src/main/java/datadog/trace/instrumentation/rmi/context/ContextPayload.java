package datadog.trace.instrumentation.rmi.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public class ContextPayload implements Serializable {
  @Getter private final Map<String, String> data;

  public ContextPayload() {
    data = new HashMap<>();
  }
}
