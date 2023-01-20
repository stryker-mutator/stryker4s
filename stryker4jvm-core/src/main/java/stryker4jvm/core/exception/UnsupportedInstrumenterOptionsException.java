package stryker4jvm.core.exception;

import stryker4jvm.core.model.InstrumenterOptions;
import stryker4jvm.core.model.LanguageMutator;

/**
 * Exception that should be thrown by a mutator when the instrumenter options provided is not supported.
 */
public class UnsupportedInstrumenterOptionsException extends LanguageMutatorProviderException {
    /**
     * Creates a generic message for this UnsupportedInstrumenterOptionsException using the provided
     * {@link InstrumenterOptions}.
     *
     * @param unsupportedOption The instrumenter option that was not supported.
     */
    public UnsupportedInstrumenterOptionsException(InstrumenterOptions unsupportedOption) {
        super(buildMessage(unsupportedOption));
    }

    private static String buildMessage(InstrumenterOptions options) {
        if (options == null)
            return "Instrumenter options was null";
        return String.format("Instrumenter options '%s' is not supported", options);
    }
}
