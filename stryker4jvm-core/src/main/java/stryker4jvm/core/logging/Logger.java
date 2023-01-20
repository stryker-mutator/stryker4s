package stryker4jvm.core.logging;

import static stryker4jvm.core.logging.LogLevel.*;

/**
 * Abstract logger used in stryker4jvm
 */
public abstract class Logger {
    private final boolean colorEnabled;

    /**
     * Creates a new logger. Whether color is enabled or not depends on {@link #determineColorEnabled()}.
     */
    public Logger() {
        colorEnabled = determineColorEnabled();
    }

    /**
     * Creates a new logger with the provided colorEnabled value and thus ignores {@link #determineColorEnabled()}.
     *
     * @param colorEnabled Whether color is enabled.
     */
    public Logger(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
    }

    public final void debug(String msg) {
        log(Debug, msg);
    }

    public final void debug(Throwable e) {
        log(Debug, e);
    }

    public final void debug(String msg, Throwable e) {
        log(Debug, msg, e);
    }

    public final void info(String msg) {
        log(Info, msg);
    }

    public final void info(Throwable e) {
        log(Info, e);
    }

    public final void info(String msg, Throwable e) {
        log(Info, msg, e);
    }

    public final void warn(String msg) {
        log(Warn, msg);
    }

    public final void warn(Throwable e) {
        log(Warn, e);
    }

    public final void warn(String msg, Throwable e) {
        log(Warn, msg, e);
    }

    public final void error(String msg) {
        log(Error, msg);
    }

    public final void error(Throwable e) {
        log(Error, e);
    }

    public final void error(String msg, Throwable e) {
        log(Error, msg, e);
    }

    public abstract void log(LogLevel level, String msg);

    public abstract void log(LogLevel level, String msg, Throwable e);

    public abstract void log(LogLevel level, Throwable e);

    /**
     * Whether the logger support color.
     *
     * @return colorEnabled (constant value).
     */
    public final boolean isColorEnabled() {
        return colorEnabled;
    }

    /**
     * Function that determines whether color is enabled on the terminal used. It does so on a best-effort basis.
     * Classes that extend from {@link #Logger} may override this function. However, the programmer must make sure that
     * this test does not depend on any field of the subclass as these may not be initialised yet when this is invoked.
     *
     * @return True if color should be enabled, false otherwise.
     */
    protected boolean determineColorEnabled() {
        // Explicitly disable color https://no-color.org/
        boolean notNoColor = System.getenv().containsKey("NO_COLOR");
        // If there is a TERM on Linux (or Windows Git Bash), assume we support color
        boolean unixEnabled = System.getenv().containsKey("TERM");
        // On Windows there's no easy way. But if we're in Windows Terminal or ConEmu, we can assume we support color
        boolean windowsEnabled =
                System.getenv().containsKey("WT_SESSION") ||
                        System.getenv().containsKey("ConEmuANSI") && System.getenv("ConEmuANSI").equals("ON");

        return notNoColor && (unixEnabled || windowsEnabled);
    }
}
