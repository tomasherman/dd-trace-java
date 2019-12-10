package datadog.trace.instrumentation.rmi.context;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ContextTransport extends Remote {
  void post(ContextPayload payload) throws RemoteException;
}
