package stryker4jvm.core.config;

import java.util.Set;

/**
 * Generic configuration used for all language-specific mutators which allows for
 * customisation of some mutator properties.
 */
public class LanguageMutatorConfig {
    private final String dialect;
    private final Set<String> excludedMutations;

    public LanguageMutatorConfig(String dialect, Set<String> excludedMutations) {
        this.dialect = dialect;
        this.excludedMutations = excludedMutations;
    }

    /**
     * Retrieves the dialect of the configuration. The dialect should indicate the version of the (programming)
     * language used.
     * @return The dialect, possibly null (none provided).
     */
    public String getDialect() {
        return dialect;
    }

    /**
     * Retrieves the set of excluded mutations. Any mutator name that is in this set should not be used by the
     * language-specific mutator.
     * @return The set of excluded mutations.
     */
    public Set<String> getExcludedMutations() {
        return excludedMutations;
    }
}
