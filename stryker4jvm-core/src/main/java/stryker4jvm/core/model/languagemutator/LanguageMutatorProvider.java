package stryker4jvm.core.model.languagemutator;

import stryker4jvm.core.config.LanguageMutatorConfig;
import stryker4jvm.core.exception.LanguageMutatorProviderException;
import stryker4jvm.core.logging.Logger;
import stryker4jvm.core.model.InstrumenterOptions;
import stryker4jvm.core.model.LanguageMutator;

/**
 * Factory-like interface for creating language mutators.
 */
public interface LanguageMutatorProvider {
    /**
     * Factory method that should return a mutator that is constrained by the config and instrumenter options.
     *
     * @param config              The configuration.
     * @param logger              The logger to use
     * @param instrumenterOptions The instrumenter options.
     * @return A LanguageMutator abiding to the provided configuration and options.
     */
    LanguageMutator<?> provideMutator(LanguageMutatorConfig config, Logger logger, InstrumenterOptions instrumenterOptions) throws LanguageMutatorProviderException;
}
