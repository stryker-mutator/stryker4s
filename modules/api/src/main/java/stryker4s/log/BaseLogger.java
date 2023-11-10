package stryker4s.log;

import java.util.function.Supplier;

public interface BaseLogger {

	public void log(Level level, Supplier<String> msg);

	public void log(Level level, Supplier<String> msg, Supplier<Throwable> t);
}
