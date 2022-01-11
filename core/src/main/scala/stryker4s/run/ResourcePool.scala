package stryker4s.run

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.std.*
import cats.syntax.functor.*
import cats.syntax.parallel.*
import fs2.Pipe

trait ResourcePool[T] {

  /** Pipe that empties the given stream against the resource pool, using a concurrency of as many resources are
    * available on the pool
    */
  def run[O, O2](f: (T, O) => IO[O2]): Pipe[IO, O, O2]

  /** Take 1 Resource from the pool. Puts the resource back into the pool when the returned resource closes (after
    * `.use`)
    */
  def loan: Resource[IO, T]

}

object ResourcePool {

  /** Create a Resource pool that can use and put resources back in the pool after use,
    */
  def apply[T](resources: NonEmptyList[Resource[IO, T]]): Resource[IO, ResourcePool[T]] =
    Resource.eval(Queue.unbounded[IO, T]).flatMap { queue =>
      // Publish all resources in a re-filling queue
      val publish = resources.parTraverse_(_.evalMap(queue.offer(_)))

      publish.as(new ResourcePool[T] {
        override def run[U, V](f: (T, U) => IO[V]): Pipe[IO, U, V] =
          _.parEvalMapUnordered(Integer.MAX_VALUE)(item => loan.use(f(_, item)))

        override def loan: Resource[IO, T] = Resource.make(queue.take)(queue.offer)
      })
    }
}
