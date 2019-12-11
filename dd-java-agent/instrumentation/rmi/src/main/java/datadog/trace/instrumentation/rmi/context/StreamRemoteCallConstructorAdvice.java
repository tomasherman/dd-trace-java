package datadog.trace.instrumentation.rmi.context;

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

    propagate(c);
  }

  public static boolean isRMIInternalObject(final ObjID id) {
    return ACTIVATOR_ID.equals(id) || DGC_ID.equals(id) || REGISTRY_ID.equals(id);
  }

  public static void propagate(final Connection c) {
    carrier.put(TRACE_ID_KEY, context.getTraceId().toString());
    carrier.put(SPAN_ID_KEY, context.getSpanId().toString());
    if (context.lockSamplingPriority()) {
      carrier.put(SAMPLING_PRIORITY_KEY, String.valueOf(context.getSamplingPriority()));
    }

    if (sendDummyCall(c, -1, 100L)) {
      sendDummyCall(c, 0, 101L);
      sendDummyCall(c, 0, 102L);
      sendDummyCall(c, 0, 103L);
    }
  }

  private static boolean sendDummyCall(
      final Connection c, final int identifier, final long payload) {
    final StreamRemoteCall shareContextCall = new StreamRemoteCall(c);
    try {
      c.getOutputStream().write(TransportConstants.Call);

      final ObjectOutput out = shareContextCall.getOutputStream();

      DD_CONTEXT_CALL_ID.write(out);

      // call header, part 2 (read by Dispatcher)
      out.writeInt(identifier); // method number (operation index)
      out.writeLong(payload); // stub/skeleton hash

      try {
        shareContextCall.executeCall();
      } catch (final Exception e) {
        final Exception ex = shareContextCall.getServerException();
        if (ex != null) {
          if (ex instanceof NoSuchObjectException) {
            // TODO: don't try to pass context in this connection - ever again
          } else {
            ex.printStackTrace();
          }
        } else {
          e.printStackTrace();
        }
        return false;
      } finally {
        shareContextCall.done();
      }

    } catch (final IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
