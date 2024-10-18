package stryker4s.sbt.testrunner

import sbt.testing
import sbt.testing.{Event, Status, TaskDef}
import stryker4s.model.MutantId
import stryker4s.testrunner.api.*

/** Maps stryker4s-api test-interface models to sbt-testinterface models
  */
protected[stryker4s] trait TestInterfaceMapper {
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
      case AnnotatedFingerprint(fIsModule, annotation) => interface.AnnotatedFingerprintImpl(fIsModule, annotation)
      case SubclassFingerprint(fIsModule, superclass, noArgs) =>
        interface.SubclassFingerprintImpl(fIsModule, superclass, noArgs)
      case Fingerprint.Empty => throw new MatchError(f)
    }

  def toCoverageMap(
      coverage: Iterable[(MutantId, Seq[TestFileId])],
      testNameIds: Map[TestFileId, TestFile]
  ): CoverageTestNameMap = {
    // Create a map of fingerprints to ids to efficiently send over the wire
    val testNames: Map[MutantId, TestNames] = coverage.map { case (mutantId, testNames) =>
      mutantId -> TestNames.of(testNames)
    }.toMap
    CoverageTestNameMap.of(testNameIds, testNames)
  }

  def toFingerprint(fp: sbt.testing.Fingerprint): Fingerprint =
    fp match {
      case a: sbt.testing.AnnotatedFingerprint => AnnotatedFingerprint.of(a.isModule(), a.annotationName())
      case s: sbt.testing.SubclassFingerprint =>
        SubclassFingerprint.of(s.isModule(), s.superclassName(), s.requireNoArgConstructor())
      case _ => throw new NotImplementedError(s"Can not map fingerprint $fp")
    }

  def testNameFromEvent(event: Event): String = {
    event.selector() match {
      case n: testing.NestedSuiteSelector => n.suiteId()
      case n: testing.NestedTestSelector  => n.testName()
      case t: testing.TestSelector        => t.testName()
      case _                              => event.fullyQualifiedName()
    }
  }

  def toOption(optionalThrowable: testing.OptionalThrowable): Option[Throwable] =
    if (optionalThrowable.isDefined()) Some(optionalThrowable.get())
    else None

}

object TestInterfaceMapper extends TestInterfaceMapper
