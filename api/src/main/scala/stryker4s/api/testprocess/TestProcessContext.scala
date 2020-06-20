package stryker4s.api.testprocess

final case class TestProcessContext(testGroups: Array[TestGroup])

sealed trait Fingerprint
final case class AnnotatedFingerprint(isModule: Boolean, annotationName: String) extends Fingerprint
final case class SubclassFingerprint(isModule: Boolean, superclassName: String, requireNoArgsConstructor: Boolean)
    extends Fingerprint

sealed trait Selector
final case class NestedSuiteSelector(suiteId: String) extends Selector
final case class NestedTestSelector(suiteId: String, testName: String) extends Selector
final case class SuiteSelector() extends Selector
final case class TestSelector(testName: String) extends Selector
final case class TestWildcardSelector(testWildcard: String) extends Selector

final case class TaskDefinition(
    fullyQualifiedName: String,
    fingerprint: Fingerprint,
    explicitlySpecified: Boolean,
    selectors: Array[Selector]
)

final case class TestGroup(frameworkClass: String, taskDefs: Array[TaskDefinition], runnerOptions: RunnerOptions)

final case class RunnerOptions(args: Array[String], remoteArgs: Array[String])
