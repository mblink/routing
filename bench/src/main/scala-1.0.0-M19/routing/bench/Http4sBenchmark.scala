package routing
package bench

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.openjdk.jmh.annotations._

object http4sHelper extends Http4sBenchmarkHelper {
  def runIO[A](io: IO[A]): A = io.unsafeRunSync()
}

class Http4sBenchmark {
  import http4sHelper._

  @Benchmark def http4s: String = run(http4sService)
  @Benchmark def routing: String = run(routingService)
  @Benchmark def routingManual: String = run(routingManualService)
}
