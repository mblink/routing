package routing
package bench

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.openjdk.jmh.annotations._
import routing.http4s._
import izumi.reflect.macrortti.LTT

object helper {
  object Id extends QueryParamDecoderMatcher[Int]("id")
  object Enabled extends QueryParamDecoderMatcher[Boolean]("enabled")

  val route1 = routing.Method.GET / "route1"
  val route2 = routing.Method.GET / "route2" / pathVar[String]("param")
  val route3 = routing.Method.POST / "route3" / pathVar[Int]("param")
  val route4 = routing.Method.GET / "route4" / "part" :? queryParam[Boolean]("enabled")
  val route5 = routing.Method.POST / pathVar[String]("param") / "route5" :? queryParam[Int]("id")

  val route1Res = () => Ok("route 1")
  val route2Res = (p: String) => Ok(s"route 2: $p")
  val route3Res = (i: Int) => Ok(s"route 3: $i")
  val route4Res = (b: Boolean) => Ok(s"route 4: $b")
  val route5Res = (s: String, i: Int) => Ok(s"route 5: $s, $i")

  val http4sService = HttpRoutes.of[IO] {
    case GET -> Root / "route1" => route1Res()
    case GET -> Root / "route2" / param => route2Res(param)
    case POST -> Root / "route3" / IntVar(param) => route3Res(param)
    case GET -> Root / "route4" / "part" :? Enabled(enabled) => route4Res(enabled)
    case POST -> Root / param / "route5" :? Id(id) => route5Res(param, id)
  }

  val routingService = Route.httpRoutes[IO](
    route1.handle.with_(_ => _ => route1Res()),
    route2.handle.with_(x => _ => route2Res(x)),
    route3.handle.with_(x => _ => route3Res(x)),
    route4.handle.with_(x => _ => route4Res(x)),
    route5.handle.with_ { case (s, i) => _ => route5Res(s, i) }
  )

  val routingManualService = HttpRoutes.of[IO] {
    case route1(_) => route1Res()
    case route2(param) => route2Res(param)
    case route3(param) => route3Res(param)
    case route4(enabled) => route4Res(enabled)
    case route5(param, id) => route5Res(param, id)
  }

  def testParams(r: Route[_, _]): r.Params =
    r.paramTpes.foldLeft((): Any)((acc, tt) => (acc, tt.tag match {
      case t if t =:= LTT[Int] => 1
      case t if t =:= LTT[String] => "test"
      case t if t =:= LTT[Boolean] => true
    })).asInstanceOf[r.Params]

  def mkRequest(r: Route[_ <: routing.Method, _]): Request[IO] =
    Request[IO](uri = r.uriRaw(testParams(r)).toHttp4s, method = r.method.toHttp4s)

  val reqs = LazyList.continually(LazyList(
    mkRequest(route1),
    mkRequest(route2),
    mkRequest(route3),
    mkRequest(route4),
    mkRequest(route5)
  )).flatten.iterator

  @inline def run(routes: HttpRoutes[IO]): String =
    routes.run(reqs.next).value.flatMap(_.get.as[String]).unsafeRunSync
}

class ServiceBenchmark {
  import helper._

  @Benchmark def http4s: String = run(http4sService)
  @Benchmark def routing: String = run(routingService)
  @Benchmark def routingManual: String = run(routingManualService)
}
