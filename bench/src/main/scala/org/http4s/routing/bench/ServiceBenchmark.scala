package org.http4s
package routing
package bench

import cats.effect.IO
import cats.instances.boolean._
import cats.instances.int._
import cats.instances.string._
import org.http4s.dsl.io._
import org.openjdk.jmh.annotations._
import scala.reflect.runtime.universe.typeOf

object helper {
  object Id extends QueryParamDecoderMatcher[Int]("id")
  object Enabled extends QueryParamDecoderMatcher[Boolean]("enabled")

  val route1 = GET / "route1"
  val route2 = GET / "route2" / ("param" -> StringVar)
  val route3 = POST / "route3" / ("param" -> IntVar)
  val route4 = GET / "route4" / "part" :? queryParam[Boolean]("enabled")
  val route5 = POST / ("param" -> StringVar) / "route5" :? queryParam[Int]("id")

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
    route2.handle.with_(_ => route2Res),
    route3.handle.with_(_ => route3Res),
    route4.handle.with_(_ => route4Res),
    route5.handle.with_(_ => route5Res.tupled)
  )

  val routingManualService = HttpRoutes.of[IO] {
    case route1(_) => route1Res()
    case route2(param) => route2Res(param)
    case route3(param) => route3Res(param)
    case route4(enabled) => route4Res(enabled)
    case route5(param, id) => route5Res(param, id)
  }

  def testParams(r: Route0): r.Params =
    r.paramTpes.foldLeft((): Any)((acc, tt) => (acc, tt.tpe match {
      case t if t =:= typeOf[Int] => 1
      case t if t =:= typeOf[String] => "test"
      case t if t =:= typeOf[Boolean] => true
    })).asInstanceOf[r.Params]

  def mkRequest(r: Route0): Request[IO] =
    Request[IO](uri = r.uriRaw(testParams(r)), method = r.method)

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
