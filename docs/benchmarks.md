---
id: benchmarks
title: Benchmarks
---

Routing is an important part of a web application so it's important to understand the performance of our implementation. We put together a simple benchmark to test the performance of routing requests through a service built in three ways:

```scala mdoc
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import _root_.routing._
import _root_.routing.http4s._

val test = GET / "test"


// 1. Built with http4s routing handler syntax
val routing = Route.httpRoutes[IO](test.handle.with_(_ => _ => Ok("test")))

// 2. Built with http4s routing manual match syntax
val routingManual = Route.httpRoutes.of[IO] { case test(_) => Ok("test") }

// 3. Built with explicit URL matches using http4s' DSL
val http4s = HttpRoutes.of[IO] {
  case GET -> Root / "test" => Ok("test")
}
```

Then ran the [`ServiceBenchmark`](@GITHUB_REPO_URL@/blob/master/bench/src/main/scala/org/http4s/routing/bench/ServiceBenchmark.scala) with the command:

```
jmh:run -i 15 -wi 15 -f1 -t1 .*
```

The results showed that while using http4s' DSL was the fastest option, the services using http4s routing weren't too far behind:

```
Benchmark                        Mode  Cnt       Score      Error  Units
ServiceBenchmark.http4s         thrpt   15  133424.366 ± 2732.459  ops/s
ServiceBenchmark.routing        thrpt   15  121594.683 ± 2412.116  ops/s
ServiceBenchmark.routingManual  thrpt   15  116674.411 ± 3518.031  ops/s
```

You can [view the full output of the JMH run here](@GITHUB_REPO_URL@/blob/master/bench/results.txt).
