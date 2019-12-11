package datadog.trace.instrumentation.rmi;

import static datadog.trace.agent.tooling.ByteBuddyElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import sun.rmi.transport.ObjectTable;
import sun.rmi.transport.StreamRemoteCall;

@AutoService(Instrumenter.class)
public class RmiContextInstrumentation extends Instrumenter.Default {

  public RmiContextInstrumentation() {
    super("rmi", "rmi-context-propagator");
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return not(isInterface())
        .and(
            safeHasSuperType(
                named(StreamRemoteCall.class.getName()).or(named(ObjectTable.class.getName()))));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "datadog.trace.instrumentation.rmi.context.StreamRemoteCallConstructorAdvice",
      "datadog.trace.instrumentation.rmi.context.ObjectTableAdvice",
      "datadog.trace.instrumentation.rmi.context.ObjectTableAdvice$EhloDispatcher",
      "datadog.trace.instrumentation.rmi.context.ObjectTableAdvice$DummyRemote"
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    final Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    // TODO make this more specific
    transformers.put(
        isConstructor()
            .and(takesArgument(0, named("sun.rmi.transport.Connection")))
            .and(takesArgument(1, named("java.rmi.server.ObjID"))),
        "datadog.trace.instrumentation.rmi.context.StreamRemoteCallConstructorAdvice");

    transformers.put(
        isMethod().and(isStatic()).and(named("getTarget")),
        "datadog.trace.instrumentation.rmi.context.ObjectTableAdvice");
    return transformers;
  }
}
