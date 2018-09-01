package stryker4s.model

import scala.meta.Term

case class FoundMutant(originalTree: Term, mutations: Term*)
