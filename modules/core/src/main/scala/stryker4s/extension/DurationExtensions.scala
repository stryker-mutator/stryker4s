package stryker4s.extension

import cats.data.Chain
import cats.data.Chain.*
import cats.syntax.all.*

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

object DurationExtensions {
  implicit final class HumanReadableExtension(val duration: Duration) extends AnyVal {
    final def toHumanReadable: String = {
      val units = Chain(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.MILLISECONDS)

      // Walk the units largest-first, emitting a part per non-zero unit.
      val timeStrings = units
        .mapAccumulate(duration.toMillis) { (rest, unit) =>
          val name = unit.toString().toLowerCase()
          val result = unit.convert(rest, TimeUnit.MILLISECONDS)
          val diff = rest - TimeUnit.MILLISECONDS.convert(result, unit)
          val str = result match {
            case 0    => none
            case 1    => s"1 ${name.init}".some // Drop last 's'
            case more => s"$more $name".some
          }
          (diff, str)
        }
        ._2
        .flattenOption

      // `Chain`'s extractors aren't exhaustivity-aware, but these arms cover empty/one/many.
      (timeStrings: @unchecked) match {
        case Chain()       => "0 seconds"
        case Chain(a)      => a
        case init :== last => init.mkString_(", ") + " and " + last
      }
    }
  }
}
