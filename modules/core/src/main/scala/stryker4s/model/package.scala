package stryker4s

import fs2.io.file.Path
import mutationtesting.MutantResult

package object model {
  type MutantResultsPerFile = Map[Path, Vector[MutantResult]]

  // The types below now live in the public `stryker4s-plugin-api` module (`stryker4s.pluginapi`), so that
  // third-party custom mutators can depend on them without pulling in all of `core`. They're re-exported here
  // (rather than moving every usage across `core`) to keep this a source-compatible relocation.
  type PlaceableTree = stryker4s.pluginapi.PlaceableTree
  val PlaceableTree: stryker4s.pluginapi.PlaceableTree.type = stryker4s.pluginapi.PlaceableTree

  type IgnoredMutationReason = stryker4s.pluginapi.IgnoredMutationReason
  val MutationExcluded: stryker4s.pluginapi.MutationExcluded.type = stryker4s.pluginapi.MutationExcluded
  type RegexParseError = stryker4s.pluginapi.RegexParseError
  val RegexParseError: stryker4s.pluginapi.RegexParseError.type = stryker4s.pluginapi.RegexParseError
  type NoRegexMutationsFound = stryker4s.pluginapi.NoRegexMutationsFound
  val NoRegexMutationsFound: stryker4s.pluginapi.NoRegexMutationsFound.type =
    stryker4s.pluginapi.NoRegexMutationsFound

  type MutantMetadata = stryker4s.pluginapi.MutantMetadata
  val MutantMetadata: stryker4s.pluginapi.MutantMetadata.type = stryker4s.pluginapi.MutantMetadata

  type MutatedCode = stryker4s.pluginapi.MutatedCode
  val MutatedCode: stryker4s.pluginapi.MutatedCode.type = stryker4s.pluginapi.MutatedCode
}
