package stryker4s

import fs2.io.file.Path
import mutationtesting.MutantResult

package object model {
  type MutantResultsPerFile = Map[Path, Vector[MutantResult]]

  // The types below now live in the public `stryker4s-plugin-api` module (`stryker4s.plugin.api`), so that
  // third-party custom mutators can depend on them without pulling in all of `core`. They're re-exported here
  // (rather than moving every usage across `core`) to keep this a source-compatible relocation.
  type PlaceableTree = stryker4s.plugin.api.PlaceableTree
  val PlaceableTree: stryker4s.plugin.api.PlaceableTree.type = stryker4s.plugin.api.PlaceableTree

  type IgnoredMutationReason = stryker4s.plugin.api.IgnoredMutationReason
  val MutationExcluded: stryker4s.plugin.api.MutationExcluded.type = stryker4s.plugin.api.MutationExcluded
  type RegexParseError = stryker4s.plugin.api.RegexParseError
  val RegexParseError: stryker4s.plugin.api.RegexParseError.type = stryker4s.plugin.api.RegexParseError
  type NoRegexMutationsFound = stryker4s.plugin.api.NoRegexMutationsFound
  val NoRegexMutationsFound: stryker4s.plugin.api.NoRegexMutationsFound.type =
    stryker4s.plugin.api.NoRegexMutationsFound

  type MutantMetadata = stryker4s.plugin.api.MutantMetadata
  val MutantMetadata: stryker4s.plugin.api.MutantMetadata.type = stryker4s.plugin.api.MutantMetadata

  type MutatedCode = stryker4s.plugin.api.MutatedCode
  val MutatedCode: stryker4s.plugin.api.MutatedCode.type = stryker4s.plugin.api.MutatedCode
}
