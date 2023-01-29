package stryker4jvm.core.exception;

/**
 * Exception that may be thrown when an issue occurs when trying to provide a language mutator.
 *
 * @see stryker4jvm.core.exception.Stryker4jvmException
 * @see stryker4jvm.core.model.languagemutator.LanguageMutatorProvider
 */
public class LanguageMutatorProviderException extends Stryker4jvmException {
  public LanguageMutatorProviderException() {}

  public LanguageMutatorProviderException(String message) {
    super(message);
  }

  public LanguageMutatorProviderException(String message, Throwable cause) {
    super(message, cause);
  }

  public LanguageMutatorProviderException(Throwable cause) {
    super(cause);
  }
}
