package stryker4jvm.core.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

public class BufferedLogger extends Logger {
  private final List<String> logs = new LinkedList<>();

  public BufferedLogger() {}

  public BufferedLogger(boolean colorEnabled) {
    super(colorEnabled);
  }

  @Override
  public void log(LogLevel level, String msg) {
    logs.add(String.format("%s :: %s", level, msg));
  }

  @Override
  public void log(LogLevel level, String msg, Throwable e) {
    logs.add(String.format("%s :: %s:\n%s", level, msg, throwableToString(e)));
  }

  @Override
  public void log(LogLevel level, Throwable e) {
    logs.add(String.format("%s :: \n%s", level, throwableToString(e)));
  }

  private String throwableToString(Throwable e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  public boolean isEmpty() {
    return logs.isEmpty();
  }

  public void clear() {
    logs.clear();
  }

  public boolean containsAnywhere(String msg) {
    for (String s : logs) {
      if (s.contains(msg)) return true;
    }
    return false;
  }
}
