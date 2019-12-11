package datadog.trace.instrumentation.rmi;

import static datadog.trace.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.rmi.context.ContextPayload.EXTRACT_CONTEXT_ONCE;

import datadog.trace.api.DDTags;
import datadog.trace.instrumentation.api.AgentScope;
import datadog.trace.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;

public class ServerAdvice {
  @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
  public static AgentScope onEnter(
      @Advice.This final Object thiz, @Advice.Origin(value = "#m") final String method) {
    final AgentSpan.Context context = EXTRACT_CONTEXT_ONCE();

    final AgentSpan span;
    if (context == null) {
      span = startSpan("rmi.request");

    } else {
      span = startSpan("rmi.request", context);
    }
    span.setTag(DDTags.RESOURCE_NAME, thiz.getClass().getSimpleName() + "#" + method)
        .setTag("span.origin.type", thiz.getClass().getCanonicalName());
    return activateSpan(span, true);
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void stopSpan(
      @Advice.Enter final AgentScope scope, @Advice.Thrown final Throwable throwable) {
    if (scope == null) {
      return;
    }
    final AgentSpan span = scope.span();
    if (throwable != null) {
      span.setError(true);
      span.addThrowable(throwable);
    }
    scope.close();
  }
}
