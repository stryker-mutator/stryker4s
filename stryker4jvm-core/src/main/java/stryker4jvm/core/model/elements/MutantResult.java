package stryker4jvm.core.model.elements;

import java.util.List;
import java.util.Optional;

/** Data container summarising the results of a mutant during a test run. */
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

  public MutantResult(
      String id,
      String mutatorName,
      String replacement,
      Location location,
      MutantStatus mutantStatus) {
    this.id = id;
    this.mutatorName = mutatorName;
    this.replacement = replacement;
    this.location = location;
    this.mutantStatus = mutantStatus;
  }

  public MutantResult(
      String id,
      String mutatorName,
      String replacement,
      Location location,
      MutantStatus mutantStatus,
      String statusReason,
      String description,
      List<String> coveredBy,
      List<String> killedBy,
      Integer testsCompleted,
      Boolean isStatic) {
    this.id = id;
    this.mutatorName = mutatorName;
    this.replacement = replacement;
    this.location = location;
    this.mutantStatus = mutantStatus;
    this.statusReason = Optional.ofNullable(statusReason);
    this.description = Optional.ofNullable(description);
    this.coveredBy = Optional.ofNullable(coveredBy);
    this.killedBy = Optional.ofNullable(killedBy);
    this.testsCompleted = Optional.ofNullable(testsCompleted);
    this.isStatic = Optional.ofNullable(isStatic);
  }
}
