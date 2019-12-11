package datadog.trace.instrumentation.rmi.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.experimental.Accessors;
import sun.rmi.transport.Channel;
import sun.rmi.transport.Connection;

public class ConnectionProxy implements Connection {
  private final Connection target;
  @Accessors private final boolean instrumentedServer;
  @Accessors private final boolean checkPerformed;

  public ConnectionProxy(final Connection target) {
    instrumentedServer = false;
    checkPerformed = false;
    this.target = target;
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
