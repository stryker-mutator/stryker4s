package stryker4jvm.plugin.sbt.testrunner

import sbt.testing.{Status, TaskDef}
import stryker4jvm.plugin.sbt.testrunner.model.{AnnotatedFingerprintImpl, SubclassFingerprintImpl}
import stryker4jvm.api.testprocess.*

/** Maps stryker4s-api test-interface models to sbt-testinterface models
  */
trait TestInterfaceMapper {
  def combineStatus(current: Status, newStatus: Status) =
    (current, newStatus) match {
      case (Status.Error, _)   => Status.Error
      case (_, Status.Error)   => Status.Error
      case (Status.Failure, _) => Status.Failure
      case (_, Status.Failure) => Status.Failure
      case _                   => Status.Success
    }

  def toSbtTaskDef(td: TaskDefinition) = {
    val fingerprint = toSbtFingerprint(td.fingerprint)
    val selectors = td.selectors.map(toSbtSelector).toArray
    new TaskDef(td.fullyQualifiedName, fingerprint, td.explicitlySpecified, selectors)
  }

  def toSbtSelector(s: Selector): sbt.testing.Selector =
    s match {
      case NestedSuiteSelector(suiteId)          => new sbt.testing.NestedSuiteSelector(suiteId)
      case NestedTestSelector(suiteId, testName) => new sbt.testing.NestedTestSelector(suiteId, testName)
      case SuiteSelector()                       => new sbt.testing.SuiteSelector()
      case TestSelector(testName)                => new sbt.testing.TestSelector(testName)
      case TestWildcardSelector(testWildcard)    => new sbt.testing.TestWildcardSelector(testWildcard)
      case Selector.Empty                        => throw new MatchError(s)
    }

  def toSbtFingerprint(f: Fingerprint): sbt.testing.Fingerprint =
    f match {
      case AnnotatedFingerprint(fIsModule, annotation) => AnnotatedFingerprintImpl(fIsModule, annotation)
      case SubclassFingerprint(fIsModule, superclass, noArgs) =>
        SubclassFingerprintImpl(fIsModule, superclass, noArgs)
      case Fingerprint.Empty => throw new MatchError(f)
    }

  def toCoverageMap(coverage: Iterable[(Int, Seq[String])]): CoverageTestNameMap = {
    // Create a map of fingerprints to ids to efficiently send over the wire
    val testNameIds = coverage.flatMap(_._2).toSet.zipWithIndex.toMap
    val testNames: Map[Int, TestNames] = coverage.map { case (id, testNames) =>
      id -> toTestNames(testNames, testNameIds)
    }.toMap
    CoverageTestNameMap(testNameIds.map(_.swap), testNames)
  }

  def toTestNames(testNames: Seq[String], testnameIds: Map[String, Int]): TestNames =
    TestNames(testNames.map(testnameIds(_)).toSeq)

  def toFingerprint(fp: sbt.testing.Fingerprint): Fingerprint =
    fp match {
      case a: sbt.testing.AnnotatedFingerprint => AnnotatedFingerprint(a.isModule(), a.annotationName())
      case s: sbt.testing.SubclassFingerprint =>
        SubclassFingerprint(s.isModule(), s.superclassName(), s.requireNoArgConstructor())
      case _ => throw new NotImplementedError(s"Can not map fingerprint $fp")
    }
}

object TestInterfaceMapper extends TestInterfaceMapper
