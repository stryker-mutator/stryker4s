package stryker4s.config
import stryker4s.extension.exception.InvalidExclusionsException
import stryker4s.extension.mutationtype.Mutation

final case class ExcludedMutations(private val _exclusions: Set[String] = Set.empty) {
  val exclusions: Set[String] = {
    val (valid, invalid) = _exclusions.partition(Mutation.mutations.contains)
    if (invalid.nonEmpty) throw InvalidExclusionsException(invalid)
    valid
  }
}
