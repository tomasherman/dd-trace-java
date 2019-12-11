package datadog.trace.instrumentation.rmi.context;

import java.io.IOException;
import java.io.ObjectInput;
import java.lang.reflect.Field;
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
    if (!isDDContextCall(oe)) {
      return;
    }

    final Remote dummy = new DummyRemote();
    final Dispatcher dispatcher = new EhloDispatcher();
    result = new Target(dummy, dispatcher, dummy, new ObjID(), false);
  }

  public static boolean isDDContextCall(final Object oe) {
    final Class<?> clazz = oe.getClass();
    try {
      // sun.rmi.transport.ObjectEndpoint is protected and field "id" is private
      final Field id = clazz.getDeclaredField("id");
      id.setAccessible(true);
      final ObjID actualId = (ObjID) id.get(oe);

      if (actualId.equals(StreamRemoteCallConstructorAdvice.DD_CONTEXT_CALL_ID)) {
        return true;
      }
    } catch (final ReflectiveOperationException e) {
      // TODO: log it
    }
    return false;
  }

  public static class DummyRemote implements Remote {}

  public static class EhloDispatcher implements Dispatcher {
    @Override
    public void dispatch(final Remote obj, final RemoteCall call) throws IOException {
      final ObjectInput in = call.getInputStream();
      in.readInt();
      System.err.println("ehlo dispatcher " + in.readLong());

      call.getResultStream(true);
      call.releaseInputStream();
      call.releaseOutputStream();
      call.done();
    }
  }
}
