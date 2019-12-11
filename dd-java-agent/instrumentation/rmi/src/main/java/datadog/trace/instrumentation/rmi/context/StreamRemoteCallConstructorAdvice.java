package datadog.trace.instrumentation.rmi.context;

import static datadog.trace.instrumentation.api.AgentTracer.activeSpan;
import static datadog.trace.instrumentation.api.AgentTracer.propagate;
import static datadog.trace.instrumentation.rmi.context.ContextPayload.SETTER;

import datadog.trace.instrumentation.api.AgentSpan;
import java.io.IOException;
import java.io.ObjectOutput;
import java.rmi.NoSuchObjectException;
import java.rmi.server.ObjID;
import net.bytebuddy.asm.Advice;
import sun.rmi.transport.Connection;
import sun.rmi.transport.StreamRemoteCall;
import sun.rmi.transport.TransportConstants;

public class StreamRemoteCallConstructorAdvice {
  public static final ObjID ACTIVATOR_ID = new ObjID(ObjID.ACTIVATOR_ID);
  public static final ObjID DGC_ID = new ObjID(ObjID.DGC_ID);
  public static final ObjID REGISTRY_ID = new ObjID(ObjID.REGISTRY_ID);
  public static final ObjID DD_CONTEXT_CALL_ID = new ObjID("Datadog.context_call".hashCode());
  public static final int CONTEXT_CHECK_CALL_OP_ID = -1;
  public static final int CONTEXT_PASS_OPERATION_ID = -2;
  public static ThreadLocal<Boolean> internalCall = new ThreadLocal<>();

  static {
    internalCall.set(false);
  }

  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static void onEnter(
      @Advice.Argument(value = 0) final Connection c, @Advice.Argument(value = 1) final ObjID id) {
    if (!c.isReusable()) {
      return;
    }
    if (isRMIInternalObject(id)) {
      return;
    }

    PROPAGATE(c);
  }

  public static boolean isRMIInternalObject(final ObjID id) {
    return ACTIVATOR_ID.equals(id) || DGC_ID.equals(id) || REGISTRY_ID.equals(id);
  }

  public static void PROPAGATE(final Connection c) {
    final AgentSpan span = activeSpan();
    if (span == null) {
      return;
    }

    if (checkIfContextCanBePassed(c)) {
      final ContextPayload payload = new ContextPayload();
      propagate().inject(span, payload, SETTER);
      syntheticCall(c, payload, CONTEXT_PASS_OPERATION_ID);
    }
  }

  private static boolean checkIfContextCanBePassed(final Connection c) {
    // TODO memorize this per connection to avoid unnecessary overhead
    return syntheticCall(c, null, CONTEXT_CHECK_CALL_OP_ID);
  }

  private static boolean syntheticCall(
      final Connection c, final Object payload, final int operationId) {
    final StreamRemoteCall shareContextCall = new StreamRemoteCall(c);
    try {
      c.getOutputStream().write(TransportConstants.Call);

      final ObjectOutput out = shareContextCall.getOutputStream();

      DD_CONTEXT_CALL_ID.write(out);

      // call header, part 2 (read by Dispatcher)
      out.writeInt(operationId); // method number (operation index)
      out.writeLong(operationId); // stub/skeleton hash

      if (payload != null) {
        out.writeObject(payload);
      }

      try {
        shareContextCall.executeCall();
      } catch (final Exception e) {
        final Exception ex = shareContextCall.getServerException();
        if (ex != null) {
          if (ex instanceof NoSuchObjectException) {
            return false;
          } else {
            // TODO: log ex.printStackTrace();
          }
        } else {
          // TODO: log ex.printStackTrace();
        }
        return false;
      } finally {
        shareContextCall.done();
      }

    } catch (final IOException e) {
      // TODO: log ex.printStackTrace();
      return false;
    }
    return true;
  }
}
