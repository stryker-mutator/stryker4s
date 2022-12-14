package stryker4jvm.core.exception;

public abstract class Stryker4jvmException extends Exception {
    public Stryker4jvmException() {
    }

    public Stryker4jvmException(Throwable cause) {
        super(cause);
    }

    public Stryker4jvmException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Stryker4jvmException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public Stryker4jvmException(String msg) {
        super(msg);
    }
}
