package stryker4s.log

import xsbti.Logger as ParentXsbtiLogger

import java.util.function.Supplier

/** Passes through [[stryker4s.log.Logger]] to xsbti.Logger
  */
class XsbtiLogger(using log: Logger) extends ParentXsbtiLogger {

  override def error(msg: Supplier[String]): Unit = log.error(msg.get())
  override def warn(msg: Supplier[String]): Unit = log.debug(msg.get())
  override def info(msg: Supplier[String]): Unit = log.debug(msg.get())
  override def debug(msg: Supplier[String]): Unit = log.debug(msg.get())
  override def trace(exception: Supplier[Throwable]): Unit = log.debug("trace", exception.get())

}
