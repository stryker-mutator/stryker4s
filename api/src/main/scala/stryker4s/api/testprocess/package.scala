package stryker4s.api

import java.{util => ju}

package object testprocess {

  type CoverageReport = ju.Map[Int, Array[Fingerprint]]

}
