package stryker4jvm.core.model;

public interface IgnoredMutationReason {
    String explanation();

    final class MutationExcluded implements IgnoredMutationReason {

        @Override
        public String explanation() {
            return "Mutation was excluded by user configuration";
        }
    }
}
