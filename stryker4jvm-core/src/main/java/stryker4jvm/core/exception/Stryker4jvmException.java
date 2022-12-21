package stryker4jvm.core.exception;

/**
 * Specific exception in stryker4jvm to be used to signal an issue
 */
public abstract class Stryker4jvmException extends Exception {
    public Stryker4jvmException(String msg) {
        super(msg);
    }

    public Stryker4jvmException() {
    }

    public Stryker4jvmException(String message, Throwable cause) {
        super(message, cause);
    }

    public Stryker4jvmException(Throwable cause) {
        super(cause);
    }
}
