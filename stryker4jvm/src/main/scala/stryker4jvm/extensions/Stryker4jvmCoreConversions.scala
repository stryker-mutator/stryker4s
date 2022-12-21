package stryker4jvm.extensions

import stryker4jvm.config.Config
import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.elements.{Location, MutantResult, MutantStatus, Position}

import scala.collection.JavaConverters.*
import java.util.Optional
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

object Stryker4jvmCoreConversions {
  implicit final class CoreMutantResultExtension(result: MutantResult) {
    def asMutationElement: mutationtesting.MutantResult = {
      mutationtesting.MutantResult(
        result.id,
        result.mutatorName,
        result.replacement,
        result.location.asMutationElement,
        result.mutantStatus.asMutationElement,
        result.statusReason.asScala,
        result.description.asScala,
        result.coveredBy.asScala.map(_.toSeq),
        result.killedBy.asScala.map(_.toSeq),
        result.testsCompleted.asScala.map(_.intValue()),
        result.isStatic.asScala.map(_.booleanValue())
      )
    }
  }

  implicit final class MutantResultExtension(result: mutationtesting.MutantResult) {
    def asCoreElement: MutantResult = {
      new MutantResult(
        result.id,
        result.mutatorName,
        result.replacement,
        result.location.asCoreElement,
        result.status.asCoreElement,
        result.statusReason.orNull,
        result.description.orNull,
        result.coveredBy.map(seqAsJavaList).orNull,
        result.killedBy.map(seqAsJavaList).orNull,
        result.testsCompleted.map(Integer.valueOf).orNull,
        result.static.map(Boolean.box).orNull
      )
    }
  }

  implicit final class JavaOptionalExtension[A](optional: Optional[A]) {
    def asScala: Option[A] = {
      if (optional.isPresent)
        Option(optional.get())
      else
        Option.empty
    }
  }

  implicit final class ScalaOptionalExtension[A](option: Option[A]) {
    def asJava: Optional[A] = {
      if (option.isDefined)
        Optional.of(option.get)
      else
        Optional.empty()
    }
  }

  implicit final class CoreMutantStatusExtension(status: MutantStatus) {
    implicit def asMutationElement: mutationtesting.MutantStatus = {
      status match {
        case MutantStatus.Killed       => mutationtesting.MutantStatus.Killed
        case MutantStatus.Survived     => mutationtesting.MutantStatus.Survived
        case MutantStatus.NoCoverage   => mutationtesting.MutantStatus.NoCoverage
        case MutantStatus.Timeout      => mutationtesting.MutantStatus.Timeout
        case MutantStatus.CompileError => mutationtesting.MutantStatus.CompileError
        case MutantStatus.RuntimeError => mutationtesting.MutantStatus.RuntimeError
        case MutantStatus.Ignored      => mutationtesting.MutantStatus.Ignored
      }
    }
  }

  implicit final class MutantStatusExtension(status: mutationtesting.MutantStatus) {
    implicit def asCoreElement: MutantStatus = status match {
      case mutationtesting.MutantStatus.Killed       => MutantStatus.Killed
      case mutationtesting.MutantStatus.Survived     => MutantStatus.Survived
      case mutationtesting.MutantStatus.NoCoverage   => MutantStatus.NoCoverage
      case mutationtesting.MutantStatus.Timeout      => MutantStatus.Timeout
      case mutationtesting.MutantStatus.CompileError => MutantStatus.CompileError
      case mutationtesting.MutantStatus.RuntimeError => MutantStatus.RuntimeError
      case mutationtesting.MutantStatus.Ignored      => MutantStatus.Ignored
    }
  }

  implicit final class CoreLocationExtension(location: Location) {
    implicit def asMutationElement: mutationtesting.Location =
      mutationtesting.Location(location.start.asMutationElement, location.end.asMutationElement)
  }

  implicit final class LocationExtension(location: mutationtesting.Location) {
    implicit def asCoreElement: Location =
      new Location(location.start.asCoreElement, location.end.asCoreElement)
  }

  implicit final class CorePositionExtension(position: Position) {
    implicit def asMutationElement: mutationtesting.Position =
      mutationtesting.Position(position.line, position.column)
  }

  implicit final class PositionExtension(position: mutationtesting.Position) {
    implicit def asCoreElement: Position = new Position(position.line, position.column)
  }

  implicit final class ConfigExtension(config: Config) {
    implicit def asLanguageMutatorConfig: LanguageMutatorConfig =
      new LanguageMutatorConfig((config.excludedMutations.asInstanceOf[Set[String]]).asJava)
  }
}
