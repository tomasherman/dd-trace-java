package datadog.trace.instrumentation.rmi;

import java.io.IOException;
import java.io.ObjectOutput;
import java.rmi.RemoteException;
import java.rmi.server.ObjID;
import net.bytebuddy.asm.Advice;
import sun.rmi.transport.Connection;
import sun.rmi.transport.StreamRemoteCall;

public class StreamRemoteCallConstructorAdvice {
  public static ThreadLocal<Boolean> internalCall = new ThreadLocal<>();

  static {
    internalCall.set(false);
  }

  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static void onEnter(
      @Advice.Argument(value = 0, readOnly = false) final Connection c,
      @Advice.Argument(value = 3) final long ifaceHash) { // TODO: add readonly
    System.err.println("ehlo");
    if (ifaceHash == 4905912898345647071L || ifaceHash == -669196253586618813L) {
      return;
    }
    if (internalCall.get()) {
      return;
    }
    if (c == null) {
      return;
    }

    if (!c.isReusable()) {
      return;
    }

    propagate(c);
  }

  public static void propagate(final Connection c) {
    internalCall.set(true);
    try {
      final StreamRemoteCall shareContextCall =
          new StreamRemoteCall(c, new ObjID(), -1, 746042503109223547L);
      final ObjectOutput out = shareContextCall.getOutputStream();
      if (out != null) {
        //        out.writeObject(new ContextPayload());
      }

      try {
        shareContextCall.executeCall();
      } catch (final Exception e) {
        final Exception ex = shareContextCall.getServerException();
        if (ex != null) {
          ex.printStackTrace();
        } else {
          e.printStackTrace();
        }
      }
      shareContextCall.done();
    } catch (final RemoteException e) {
      e.printStackTrace();
      // TODO: log this
    } catch (final IOException e) {
      e.printStackTrace();
      // TODO e.printStackTrace();
    }
    internalCall.set(false);
  }
}
