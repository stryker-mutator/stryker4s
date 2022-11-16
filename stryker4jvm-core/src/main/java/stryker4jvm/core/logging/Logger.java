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
        logImpl(DEBUG, msg);
    }

    public final void debug(Throwable e) {
        logImpl(DEBUG, e);
    }

    public final void debug(String msg, Throwable e) {
        logImpl(DEBUG, msg, e);
    }

    public final void info(String msg) {
        logImpl(INFO, msg);
    }

    public final void info(Throwable e) {
        logImpl(INFO, e);
    }

    public final void info(String msg, Throwable e) {
        logImpl(INFO, msg, e);
    }

    public final void warn(String msg) {
        logImpl(WARN, msg);
    }

    public final void warn(Throwable e) {
        logImpl(WARN, e);
    }

    public final void warn(String msg, Throwable e) {
        logImpl(WARN, msg, e);
    }

    public final void error(String msg) {
        logImpl(ERROR, msg);
    }

    public final void error(Throwable e) {
        logImpl(ERROR, e);
    }

    public final void error(String msg, Throwable e) {
        logImpl(ERROR, msg, e);
    }

    private String processMsg(String msg) {
        return colorEnabled ? fansi.Str.implicitApply(msg).render() : msg;
    }

    private void logImpl(LogLevel level, String msg) {
        log(level, processMsg(msg));
    }

    private void logImpl(LogLevel level, String msg, Throwable e) {
        log(level, processMsg(msg), e);
    }

    private void logImpl(LogLevel level, Throwable e) {
        log(level, e);
    }

    public abstract void log(LogLevel level, String msg);

    public abstract void log(LogLevel level, String msg, Throwable e);

    public abstract void log(LogLevel level, Throwable e);

    private boolean determineColorEnabled() {
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
