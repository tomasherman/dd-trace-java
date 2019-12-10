package datadog.trace.instrumentation.rmi.context;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ContextServer extends UnicastRemoteObject implements ContextTransport {
  protected ContextServer() throws RemoteException {}

  @Override
  public void post(final ContextPayload payload) throws RemoteException {
    System.err.println("context posted");
  }
}
