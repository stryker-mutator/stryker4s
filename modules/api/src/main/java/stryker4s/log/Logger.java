package stryker4s.log;

import java.util.function.Supplier;

public interface Logger {

	public void log(Level level, Supplier<String> msg);

	public void log(Level level, Supplier<String> msg, Throwable t);

  /** 
	 * Whether colors are enabled in the log
    */
	public default boolean colorEnabled() {
		var env = System.getenv();
    // Explicitly disable color https://no-color.org/
    boolean notNoColor = !env.containsKey("NO_COLOR");
    // If there is a TERM on Linux (or Windows Git Bash), assume we support color
    boolean unixEnabled = env.containsKey("TERM");
    // On Windows there's no easy way. But if we're in Windows Terminal or ConEmu, we can assume we support color
    boolean windowsEnabled = env.containsKey("WT_SESSION") || (System.getenv().containsKey("ConEmuANSI") && System.getenv("ConEmuANSI").equals("ON"));

    return notNoColor && (unixEnabled || windowsEnabled);
	}
}
