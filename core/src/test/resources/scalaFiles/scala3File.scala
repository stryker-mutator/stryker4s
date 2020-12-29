// Tests support for new top-level and extension syntax

object SeqOps {
  extension[A, B](list: Seq[A]) {
    def myFoldRight(init: B)(f: (A, B) => B): B = list.foldRight(init)(f)
  }
}

type Foo = String
