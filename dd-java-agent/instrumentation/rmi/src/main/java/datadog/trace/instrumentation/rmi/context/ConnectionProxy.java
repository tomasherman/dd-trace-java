package datadog.trace.instrumentation.rmi.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import sun.rmi.transport.Channel;
import sun.rmi.transport.Connection;

public class ConnectionProxy implements Connection {
  final Connection target;

  public ConnectionProxy(final Connection target) {
    this.target = target;
  }

  public boolean isConnectedToInstrumentedServer() {
    return true;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return target.getInputStream();
  }

  @Override
  public void releaseInputStream() throws IOException {
    target.releaseInputStream();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {

    return target.getOutputStream();
  }

  @Override
  public void releaseOutputStream() throws IOException {
    target.releaseOutputStream();
  }

  @Override
  public boolean isReusable() {
    return target.isReusable();
  }

  @Override
  public void close() throws IOException {
    target.close();
  }

  @Override
  public Channel getChannel() {
    return target.getChannel();
  }
}
