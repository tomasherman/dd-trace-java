package datadog.trace.instrumentation.rmi.context;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamProxy extends OutputStream {
  private final OutputStream target;

  public OutputStreamProxy(final OutputStream target) {
    this.target = target;
  }

  @Override
  public void write(final int b) throws IOException {
    target.write(b);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    target.write(b, off, len);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    target.write(b);
  }

  @Override
  public void close() throws IOException {}

  @Override
  public void flush() throws IOException {}
}
