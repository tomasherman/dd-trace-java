package datadog.trace.instrumentation.rmi.context;

import java.io.IOException;
import java.io.ObjectInput;
import java.rmi.Remote;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteCall;
import net.bytebuddy.asm.Advice;
import sun.rmi.server.Dispatcher;
import sun.rmi.transport.Target;

public class ObjectTableAdvice {
  @Advice.OnMethodExit(suppress = Throwable.class)
  public static void methodExit(
      @Advice.Argument(0) final Object oe, @Advice.Return(readOnly = false) Target result) {
    final Remote dummy = new DummyRemote();
    final Dispatcher dispatcher = new EhloDispatcher();
    final Target target = new Target(dummy, dispatcher, dummy, new ObjID(), false);
    if (result == null) {
      result = target;
    }
  }

  public static class DummyRemote implements Remote {}

  public static class EhloDispatcher implements Dispatcher {
    @Override
    public void dispatch(final Remote obj, final RemoteCall call) throws IOException {
      System.err.println("ehlo dispatcher");
      final ObjectInput in = call.getInputStream();
      in.readInt();
      in.readLong();
      call.getResultStream(true);
      call.releaseInputStream();
      call.releaseOutputStream();
      call.done();
    }
  }
}
