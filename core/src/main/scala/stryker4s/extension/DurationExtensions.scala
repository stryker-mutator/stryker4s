package stryker4s.extension

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

object DurationExtensions {
  implicit class HumanReadableExtension(duration: Duration) {
    final def toHumanReadable: String = {
      val units = Seq(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.MILLISECONDS)

      val timeStrings = units
        .foldLeft((Seq.empty[String], duration.toMillis))({ case ((humanReadable, rest), unit) =>
          val name = unit.toString().toLowerCase()
          val result = unit.convert(rest, TimeUnit.MILLISECONDS)
          val diff = rest - TimeUnit.MILLISECONDS.convert(result, unit)
          val str = result match {
            case 0    => humanReadable
            case 1    => humanReadable :+ s"1 ${name.init}" // Drop last 's'
            case more => humanReadable :+ s"$more $name"
          }
          (str, diff)
        })
        ._1

      timeStrings.size match {
        case 0 => "0 seconds"
        case 1 => timeStrings.head
        case _ => timeStrings.init.mkString(", ") + " and " + timeStrings.last
      }
    }
  }
}
