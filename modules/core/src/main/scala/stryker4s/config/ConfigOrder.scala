package stryker4s.config

import cats.Order

/** Abstract unit to represent in which order a config source should be loaded. The lower the value, the higher the
  * priority.
  */
final case class ConfigOrder(value: Int) extends AnyVal

object ConfigOrder {
  def Last = ConfigOrder(Int.MaxValue)

  implicit def order: Order[ConfigOrder] = Order.by(_.value)

}
