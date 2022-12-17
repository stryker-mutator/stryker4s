package stryker4jvm.core.model;

public interface IgnoredMutationReason {
    /**
     * @return The reason why the mutation is ignored.
     */
    String explanation();

    final class MutationExcluded implements IgnoredMutationReason {

        @Override
        public String explanation() {
            return "Mutation was excluded by user configuration";
        }
    }
}
