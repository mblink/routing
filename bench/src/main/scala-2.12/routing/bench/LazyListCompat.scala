package routing

trait LazyListCompat {
  type LazyList[A] = Stream[A]
  val LazyList = Stream
}
