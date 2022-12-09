package stryker4jvm.core.model.elements;


import java.util.List;
import java.util.Optional;

public class MutantResult {
    public String id;
    public String mutatorName;
    public String replacement;
    public Location location;
    public MutantStatus mutantStatus;
    public Optional<String> statusReason = Optional.empty();
    public Optional<String> description = Optional.empty();
    public Optional<List<String>> coveredBy = Optional.empty();
    public Optional<List<String>> killedBy = Optional.empty();
    public Optional<Integer> testsCompleted = Optional.empty();
    public Optional<Boolean> isStatic = Optional.empty();

    public MutantResult(String id, String mutatorName, String replacement, Location location, MutantStatus mutantStatus) {
        this.id = id;
        this.mutatorName = mutatorName;
        this.replacement = replacement;
        this.location = location;
        this.mutantStatus = mutantStatus;
    }

    public MutantResult(String id, String mutatorName, String replacement, Location location, MutantStatus mutantStatus,
                        String statusReason, String description, List<String> coveredBy, List<String> killedBy,
                        Integer testsCompleted, Boolean isStatic) {
        this.id = id;
        this.mutatorName = mutatorName;
        this.replacement = replacement;
        this.location = location;
        this.mutantStatus = mutantStatus;
        this.statusReason = Optional.of(statusReason);
        this.description = Optional.of(description);
        this.coveredBy = Optional.of(coveredBy);
        this.killedBy = Optional.of(killedBy);
        this.testsCompleted = Optional.of(testsCompleted);
        this.isStatic = Optional.of(isStatic);
    }
}
