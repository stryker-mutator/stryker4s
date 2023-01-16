package stryker4jvm.core.exception;

import stryker4jvm.core.model.InstrumenterOptions;
import stryker4jvm.core.model.LanguageMutator;

public class UnsupportedInstrumenterOptionsException extends LanguageMutatorProviderException {
    public UnsupportedInstrumenterOptionsException(InstrumenterOptions unsupportedOption) {
        super(buildMessage(unsupportedOption));
    }

    private static String buildMessage(InstrumenterOptions options) {
        if (options == null)
            return "Instrumenter options was null";
        return String.format("Instrumenter options '%s' is not supported", options);
    }
}
