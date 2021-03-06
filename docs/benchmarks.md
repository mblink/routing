---
id: benchmarks
title: Benchmarks
---

Routing is an important part of a web application so it's important to understand the performance of our implementation.
We put together a simple benchmark to test the performance of routing requests through a service built in three ways:

```scala mdoc
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import _root_.routing._
import _root_.routing.http4s._

val test = GET / "test"

// 1. Built with routing handler syntax
val routing = Route.httpRoutes[IO](test.handle(_ => _ => Ok("test")))

// 2. Built with routing manual match syntax
val routingManual = HttpRoutes.of[IO] { case test(_) => Ok("test") }

// 3. Built with explicit URL matches using the http4s DSL
val http4s = HttpRoutes.of[IO] {
  case GET -> Root / "test" => Ok("test")
}
```

Then ran the benchmarks with the command:

```
bench/jmh:run -i 15 -wi 15 -f1 -t1 .*
```

The results showed that while using each library's provided DSL was the fastest option, the services using our routing
mechanics still provided acceptable results:

```
Benchmark                       Mode  Cnt       Score      Error  Units
Http4sBenchmark.http4s         thrpt   15  158609.122 ±  744.871  ops/s
Http4sBenchmark.routing        thrpt   15  131537.730 ± 1006.966  ops/s
Http4sBenchmark.routingManual  thrpt   15  131961.089 ±  859.587  ops/s
PlayBenchmark.play             thrpt   15   44662.132 ±  462.867  ops/s
PlayBenchmark.routing          thrpt   15   31126.418 ±  363.973  ops/s
PlayBenchmark.routingManual    thrpt   15   31077.032 ±  213.056  ops/s

Benchmark                        Mode  Cnt       Score      Error  Units
ServiceBenchmark.http4s         thrpt   15  133424.366 ± 2732.459  ops/s
ServiceBenchmark.routing        thrpt   15  121594.683 ± 2412.116  ops/s
ServiceBenchmark.routingManual  thrpt   15  116674.411 ± 3518.031  ops/s
```

You can [view the full output of the JMH run here](@GITHUB_REPO_URL@/blob/master/bench/results.txt).
