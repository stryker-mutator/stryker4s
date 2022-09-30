package stryker4s.model

import cats.syntax.option.*
import sbt.testing.Framework as SbtFramework
import sbt.{TestDefinition as SbtTestDefinition, TestFramework as SbtTestFramework, Tests}
import stryker4s.api.testprocess.*

trait TestInterfaceMapper {
  def toApiTestGroups(frameworks: Seq[SbtFramework], sbtTestGroups: Seq[Tests.Group]): Array[TestGroup] = {
    val mapped = testMap(frameworks, sbtTestGroups.flatMap(_.tests))
    mapped.map { case (framework, tests) =>
      val taskDefs: Array[TaskDefinition] = tests.map(toTaskDefinition).toArray
      val runnerOptions = RunnerOptions(Seq.empty, Seq.empty)
      TestGroup(framework.getClass.getCanonicalName(), taskDefs, runnerOptions.some)
    }.toArray
  }

  /** From https://github.com/sbt/sbt/blob/develop/testing/src/main/scala/sbt/TestFramework.scala
    */
  private def testMap(
      frameworks: Seq[SbtFramework],
      tests: Seq[SbtTestDefinition]
  ): Map[SbtFramework, Set[SbtTestDefinition]] = {
    import scala.collection.mutable.{HashMap, HashSet, Set}
    val map = new HashMap[SbtFramework, Set[SbtTestDefinition]]
    def assignTest(test: SbtTestDefinition): Unit = {
      def isTestForFramework(framework: SbtFramework) =
        SbtTestFramework.getFingerprints(framework).exists { t =>
          SbtTestFramework.matches(t, test.fingerprint)
        }
      for (framework <- frameworks.find(isTestForFramework))
        map.getOrElseUpdate(framework, new HashSet[SbtTestDefinition]) += test
    }
    if (frameworks.nonEmpty)
      for (test <- tests) assignTest(test)
    map.toMap.mapValues(_.toSet)
  }

  private def toTaskDefinition(td: SbtTestDefinition): TaskDefinition = {
    val fingerprint = toFingerprint(td.fingerprint)
    val selectors = td.selectors.map(toSelector)
    TaskDefinition(td.name, fingerprint, td.explicitlySpecified, selectors)
  }

  private def toFingerprint(fp: sbt.testing.Fingerprint): Fingerprint =
    fp match {
      case a: sbt.testing.AnnotatedFingerprint => AnnotatedFingerprint(a.isModule(), a.annotationName())
      case s: sbt.testing.SubclassFingerprint =>
        SubclassFingerprint(s.isModule(), s.superclassName(), s.requireNoArgConstructor())
    }

  private def toSelector(s: sbt.testing.Selector): Selector =
    s match {
      case a: sbt.testing.NestedSuiteSelector  => NestedSuiteSelector(a.suiteId())
      case a: sbt.testing.NestedTestSelector   => NestedTestSelector(a.suiteId(), a.testName())
      case _: sbt.testing.SuiteSelector        => SuiteSelector()
      case a: sbt.testing.TestSelector         => TestSelector(a.testName())
      case a: sbt.testing.TestWildcardSelector => TestWildcardSelector(a.testWildcard())
    }
}
