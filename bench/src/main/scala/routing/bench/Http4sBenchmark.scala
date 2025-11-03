package routing
package bench

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.dsl.io._
import org.openjdk.jmh.annotations._
import routing.http4s._

@State(Scope.Benchmark)
object http4sHelper extends BenchmarkHelper[Request[IO], Request[IO] => IO[Response[IO]], HttpRoutes[IO]] {
  object Id extends QueryParamDecoderMatcher[Int]("id")
  object Enabled extends QueryParamDecoderMatcher[Boolean]("enabled")

  def actionRes(s: String): Request[IO] => IO[Response[IO]] = _ => Ok(s)

  def router(handlers: Handled[Request[IO] => IO[Response[IO]]]*): HttpRoutes[IO] = Route.httpRoutes[IO](handlers:_*)
  def manualRouter(pf: PartialFunction[Request[IO], Request[IO] => IO[Response[IO]]]): HttpRoutes[IO] =
    HttpRoutes.of[IO](Function.unlift((r: Request[IO]) => pf.lift(r).map(_(r))))

  val http4sService = HttpRoutes.of[IO] {
    case r @ GET -> Root / "route1" => route1Res()(r)
    case r @ GET -> Root / "route2" / param => route2Res(param)(r)
    case r @ POST -> Root / "route3" / IntVar(param) => route3Res(param)(r)
    case r @ GET -> Root / "route4" / "part" :? Enabled(enabled) => route4Res(enabled)(r)
    case r @ POST -> Root / param / "route5" :? Id(id) => route5Res(param, id)(r)
  }

  def request(r: Route[_ <: routing.Method, _]): Request[IO] =
    Request[IO](uri = r.uriRaw(testParams(r)).toHttp4s, method = r.method.toHttp4s)

  def runIO[A](io: IO[A]): A = io.unsafeRunSync()

  def runReq(request: Request[IO], routes: HttpRoutes[IO]): String =
    runIO(routes.run(request).value.flatMap(_.get.as[String]))
}

class Http4sBenchmark_0_23 { // 0.23
class Http4sBenchmark_1_0_0_M44 { // 1.0.0-M46
  import http4sHelper._

  @Benchmark def http4s: String = run(http4sService)
  @Benchmark def routing: String = run(routingService)
  @Benchmark def routingManual: String = run(routingManualService)
}
