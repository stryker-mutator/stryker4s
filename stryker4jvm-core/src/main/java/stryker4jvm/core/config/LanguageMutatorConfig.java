package stryker4jvm.core.config;

import java.util.Set;

public class LanguageMutatorConfig {
    private final String dialect;
    private final Set<String> excludedMutations;

    public LanguageMutatorConfig(String dialect, Set<String> excludedMutations) {
        this.dialect = dialect;
        this.excludedMutations = excludedMutations;
    }

    public String getDialect() {
        return dialect;
    }

    public Set<String> getExcludedMutations() {
        return excludedMutations;
    }
}
