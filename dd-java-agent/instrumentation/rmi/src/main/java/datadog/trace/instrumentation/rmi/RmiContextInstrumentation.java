package datadog.trace.instrumentation.rmi;

import static datadog.trace.agent.tooling.ByteBuddyElementMatchers.safeHasSuperType;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import sun.rmi.transport.StreamRemoteCall;

@AutoService(Instrumenter.class)
public class RmiContextInstrumentation extends Instrumenter.Default {

  public RmiContextInstrumentation() {
    super("rmi", "rmi-context-propagator");
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return not(isInterface()).and(safeHasSuperType(named(StreamRemoteCall.class.getName())));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "datadog.trace.instrumentation.rmi.StreamRemoteCallConstructorAdvice",
      "datadog.trace.instrumentation.rmi.context.ContextPayload",
      "datadog.trace.instrumentation.rmi.context.ContextServer",
      "datadog.trace.instrumentation.rmi.context.ContextServer_Stub",
      "datadog.trace.instrumentation.rmi.context.ContextTransport"
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isConstructor()
            .and(takesArgument(0, named("sun.rmi.transport.Connection")))
            .and(takesArgument(1, named("java.rmi.server.ObjID"))),
        "datadog.trace.instrumentation.rmi.StreamRemoteCallConstructorAdvice");
  }
}
