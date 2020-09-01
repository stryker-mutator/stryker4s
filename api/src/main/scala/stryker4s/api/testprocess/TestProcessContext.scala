package stryker4s.api.testprocess

@SerialVersionUID(5212069436770507771L)
final case class TestProcessContext(testGroups: Array[TestGroup])

sealed trait Fingerprint
@SerialVersionUID(3112062627280882148L)
final case class AnnotatedFingerprint(isModule: Boolean, annotationName: String) extends Fingerprint
@SerialVersionUID(3776970666899973700L)
final case class SubclassFingerprint(isModule: Boolean, superclassName: String, requireNoArgsConstructor: Boolean)
    extends Fingerprint

sealed trait Selector
@SerialVersionUID(2348199724190982135L)
final case class NestedSuiteSelector(suiteId: String) extends Selector
@SerialVersionUID(8734224844453974413L)
final case class NestedTestSelector(suiteId: String, testName: String) extends Selector
@SerialVersionUID(4075153456939218874L)
final case class SuiteSelector() extends Selector
@SerialVersionUID(6047945415418824460L)
final case class TestSelector(testName: String) extends Selector
@SerialVersionUID(2120034022429102724L)
final case class TestWildcardSelector(testWildcard: String) extends Selector

@SerialVersionUID(4220385698438861160L)
final case class TaskDefinition(
    fullyQualifiedName: String,
    fingerprint: Fingerprint,
    explicitlySpecified: Boolean,
    selectors: Array[Selector]
)

@SerialVersionUID(7377193628937705593L)
final case class TestGroup(frameworkClass: String, taskDefs: Array[TaskDefinition], runnerOptions: RunnerOptions)

@SerialVersionUID(4201390377840857593L)
final case class RunnerOptions(args: Array[String], remoteArgs: Array[String])
