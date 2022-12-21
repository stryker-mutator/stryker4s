package stryker4jvm.core.config;

import java.util.Set;

public class LanguageMutatorConfig {
    private final Set<String> excludedMutations;

    public LanguageMutatorConfig(Set<String> excludedMutations) {
        this.excludedMutations = excludedMutations;
    }

    public Set<String> getExcludedMutations() {
        return excludedMutations;
    }
}
