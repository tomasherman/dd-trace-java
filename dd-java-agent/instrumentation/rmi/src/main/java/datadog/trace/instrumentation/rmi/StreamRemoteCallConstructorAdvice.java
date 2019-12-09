package datadog.trace.instrumentation.rmi;

import net.bytebuddy.asm.Advice;

public class StreamRemoteCallConstructorAdvice {
  @Advice.OnMethodEnter(suppress = Throwable.class, inline = true)
  public static void onEnter() {
    System.err.println("ehlo");
  }
}
