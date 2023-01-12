package stryker4jvm.core.logging;

import static stryker4jvm.core.logging.LogLevel.*;

public abstract class Logger {
    protected boolean colorEnabled;

    public Logger() {
        colorEnabled = determineColorEnabled();
    }

    public Logger(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
    }

    public final void debug(String msg) {
        logImpl(Debug, msg);
    }

    public final void debug(Throwable e) {
        logImpl(Debug, e);
    }

    public final void debug(String msg, Throwable e) {
        logImpl(Debug, msg, e);
    }

    public final void info(String msg) {
        logImpl(Info, msg);
    }

    public final void info(Throwable e) {
        logImpl(Info, e);
    }

    public final void info(String msg, Throwable e) {
        logImpl(Info, msg, e);
    }

    public final void warn(String msg) {
        logImpl(Warn, msg);
    }

    public final void warn(Throwable e) {
        logImpl(Warn, e);
    }

    public final void warn(String msg, Throwable e) {
        logImpl(Warn, msg, e);
    }

    public final void error(String msg) {
        logImpl(Error, msg);
    }

    public final void error(Throwable e) {
        logImpl(Error, e);
    }

    public final void error(String msg, Throwable e) {
        logImpl(Error, msg, e);
    }

    private void logImpl(LogLevel level, String msg) {
        log(level, msg);
    }

    private void logImpl(LogLevel level, String msg, Throwable e) {
        log(level, msg, e);
    }

    private void logImpl(LogLevel level, Throwable e) {
        log(level, e);
    }

    public abstract void log(LogLevel level, String msg);

    public abstract void log(LogLevel level, String msg, Throwable e);

    public abstract void log(LogLevel level, Throwable e);

    public boolean isColorEnabled() {
        return colorEnabled;
    }

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
