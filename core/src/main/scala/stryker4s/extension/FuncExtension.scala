package stryker4s.extension

object FuncExtensions {
  implicit class ButFirstExtension[T](thisFunc: T) {

    def butFirst(fn: T => _): T = {
      fn(thisFunc)
      thisFunc
    }
  }
}
