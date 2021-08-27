package stryker4s.sbt.testrunner

import sbt.testing.{Status, TaskDef}
import stryker4s.api.testprocess._

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
      case NestedSuiteSelector(suiteId, _)          => new sbt.testing.NestedSuiteSelector(suiteId)
      case NestedTestSelector(suiteId, testName, _) => new sbt.testing.NestedTestSelector(suiteId, testName)
      case SuiteSelector(_)                         => new sbt.testing.SuiteSelector()
      case TestSelector(testName, _)                => new sbt.testing.TestSelector(testName)
      case TestWildcardSelector(testWildcard, _)    => new sbt.testing.TestWildcardSelector(testWildcard)
      case Selector.Empty                           => throw new MatchError(s)
    }

  def toSbtFingerprint(f: Fingerprint): sbt.testing.Fingerprint =
    f match {
      case AnnotatedFingerprint(fIsModule, annotation, _) =>
        new sbt.testing.AnnotatedFingerprint() {
          def isModule(): Boolean = fIsModule
          def annotationName(): String = annotation
        }
      case SubclassFingerprint(fIsModule, superclass, noArgs, _) =>
        new sbt.testing.SubclassFingerprint() {
          def isModule(): Boolean = fIsModule

          def superclassName(): String = superclass

          def requireNoArgConstructor(): Boolean = noArgs

        }
      case Fingerprint.Empty => throw new MatchError(f)
    }

  def toFingerprint(fp: sbt.testing.Fingerprint): Fingerprint =
    fp match {
      case a: sbt.testing.AnnotatedFingerprint => AnnotatedFingerprint(a.isModule(), a.annotationName())
      case s: sbt.testing.SubclassFingerprint =>
        SubclassFingerprint(s.isModule(), s.superclassName(), s.requireNoArgConstructor())
      case _ => throw new NotImplementedError(s"Can not map fingerprint $fp")
    }
}

object TestInterfaceMapper extends TestInterfaceMapper
