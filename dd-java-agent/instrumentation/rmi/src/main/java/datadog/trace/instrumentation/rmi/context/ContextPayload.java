package datadog.trace.instrumentation.rmi.context;

import static datadog.trace.instrumentation.api.AgentTracer.propagate;

import datadog.trace.instrumentation.api.AgentPropagation;
import datadog.trace.instrumentation.api.AgentSpan;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public class ContextPayload implements Serializable {
  public static final ThreadLocal<ContextPayload> THREAD_PAYLOAD = new ThreadLocal<>();

  @Getter private final Map<String, String> context;
  public static final ExtractAdapter GETTER = new ExtractAdapter();
  public static final InjectAdapter SETTER = new InjectAdapter();

  public static AgentSpan.Context EXTRACT_CONTEXT_ONCE() {
    final ContextPayload payload = THREAD_PAYLOAD.get();
    if (payload == null) {
      return null;
    }
    //    THREAD_PAYLOAD.remove();

    return propagate().extract(payload, GETTER);
  }

  public ContextPayload() {
    context = new HashMap<>();
  }

  public static class ExtractAdapter implements AgentPropagation.Getter<ContextPayload> {
    @Override
    public Iterable<String> keys(final ContextPayload carrier) {
      return carrier.getContext().keySet();
    }

    @Override
    public String get(final ContextPayload carrier, final String key) {
      return carrier.getContext().get(key);
    }
  }

  public static class InjectAdapter implements AgentPropagation.Setter<ContextPayload> {
    @Override
    public void set(final ContextPayload carrier, final String key, final String value) {
      carrier.getContext().put(key, value);
    }
  }
}
