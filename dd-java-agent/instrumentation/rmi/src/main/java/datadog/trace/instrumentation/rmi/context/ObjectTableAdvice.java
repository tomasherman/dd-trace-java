package datadog.trace.instrumentation.rmi.context;

import static datadog.trace.instrumentation.rmi.context.ContextPayload.THREAD_PAYLOAD;
import static datadog.trace.instrumentation.rmi.context.StreamRemoteCallConstructorAdvice.DD_CONTEXT_CALL_ID;

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

  public static final ContextDispatcher CONTEXT_DISPATCHER = new ContextDispatcher();
  public static final DummyRemote DUMMY_REMOTE = new DummyRemote();

  @Advice.OnMethodExit(suppress = Throwable.class)
  public static void methodExit(
      @Advice.Argument(0) final Object oe, @Advice.Return(readOnly = false) Target result) {
    final ObjID objID = GET_OBJ_ID(oe);
    if (!DD_CONTEXT_CALL_ID.equals(objID)) {
      return;
    }

    result = new Target(DUMMY_REMOTE, CONTEXT_DISPATCHER, DUMMY_REMOTE, objID, false);
  }

  public static ObjID GET_OBJ_ID(final Object oe) {
    try {
      final Class<?> clazz = oe.getClass();
      // sun.rmi.transport.ObjectEndpoint is protected and field "id" is private
      final Field id = clazz.getDeclaredField("id");
      id.setAccessible(true);
      return (ObjID) id.get(oe);
    } catch (final ReflectiveOperationException e) {
      // TODO: log it
    }
    return null;
  }

  public static class DummyRemote implements Remote {}

  public static class ContextDispatcher implements Dispatcher {
    @Override
    public void dispatch(final Remote obj, final RemoteCall call) throws IOException {
      final ObjectInput in = call.getInputStream();
      final int operationId = in.readInt();
      in.readLong(); // skip 8 bytes

      if (operationId == StreamRemoteCallConstructorAdvice.CONTEXT_PASS_OPERATION_ID) {
        try {
          final Object payload = in.readObject();
          if (payload instanceof ContextPayload) {
            // FIXME: UGHLY hack number 2001 and also ensure whatever thread local is used - its
            // sharing classloader too
            THREAD_PAYLOAD.set((ContextPayload) payload);
          }
        } catch (final ClassNotFoundException e) {
          // TODO log e.printStackTrace();
        }
      }

      // send result stream the client is expecting
      call.getResultStream(true);

      // release held streams to allow next call to continue
      call.releaseInputStream();
      call.releaseOutputStream();
      call.done();
    }
  }
}
