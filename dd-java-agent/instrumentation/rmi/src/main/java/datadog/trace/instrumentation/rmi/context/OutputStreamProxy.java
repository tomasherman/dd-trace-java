package datadog.trace.instrumentation.rmi.context;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamProxy extends OutputStream {

  @Override
  public void write(final int b) throws IOException {}
}
