// ++ 1.0.0-M30
package routing
package bench

import org.openjdk.jmh.annotations._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

@State(Scope.Thread)
class PlayBenchmark {
  import PlayBenchmarkHelper._

  @TearDown(Level.Trial) def teardown(): Unit = Await.result(actorSystem.terminate().map(_ => ()), Duration.Inf)

  @Benchmark def play: String = run(playService)
  @Benchmark def routing: String = run(routingService)
  @Benchmark def routingManual: String = run(routingManualService)
}
// -- 1.0.0-M30
