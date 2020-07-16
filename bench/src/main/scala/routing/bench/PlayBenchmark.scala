package routing
package bench

import akka.actor.ActorSystem
import izumi.reflect.macrortti.LightTypeTag
import org.openjdk.jmh.annotations._
import _root_.play.api.libs.typedmap.TypedMap
import _root_.play.api.mvc.{ActionBuilder, EssentialAction, Headers, RequestHeader, Results}
import _root_.play.api.mvc.request.{RemoteConnection, RequestTarget}
import _root_.play.api.routing.Router
import _root_.play.api.routing.sird._
import routing.play._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object playHelper {
  implicit val actorSystem: ActorSystem = ActorSystem.create()
  val action = new ActionBuilder.IgnoringBody()

  val route1 = Method.GET / "route1"
  val route2 = Method.GET / "route2" / pathVar[String]("param")
  val route3 = Method.POST / "route3" / pathVar[Int]("param")
  val route4 = Method.GET / "route4" / "part" :? queryParam[Boolean]("enabled")
  val route5 = Method.POST / pathVar[String]("param") / "route5" :? queryParam[Int]("id")

  val route1Res = () => action(Results.Ok("route 1"))
  val route2Res = (p: String) => action(Results.Ok(s"route 2: $p"))
  val route3Res = (i: Int) => action(Results.Ok(s"route 3: $i"))
  val route4Res = (b: Boolean) => action(Results.Ok(s"route 4: $b"))
  val route5Res = (s: String, i: Int) => action(Results.Ok(s"route 5: $s, $i"))

  val playService = Router.from {
    case GET(p"/route1") => route1Res()
    case GET(p"/route2/$param") => route2Res(param)
    case POST(p"/route3/${int(param)}") => route3Res(param)
    case GET(p"/route4/part" ? q"enabled=${bool(enabled)}") => route4Res(enabled)
    case POST(p"/$param/route5" ? q"id=${int(id)}") => route5Res(param, id)
  }

  val routingService = Route.router(
    route1.handle.with_(_ => route1Res()),
    route2.handle.with_(x => route2Res(x)),
    route3.handle.with_(x => route3Res(x)),
    route4.handle.with_(x => route4Res(x)),
    route5.handle.with_ { case (s, i) => route5Res(s, i) }
  )

  val routingManualService = Router.from {
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

  def mkRequest(r: Route[_ <: Method, _]): RequestHeader =
    new RequestHeader {
      def attrs = TypedMap.empty
      def connection: RemoteConnection = RemoteConnection("", false, None)
      def headers: Headers = Headers()
      def method: String = r.method.name
      def target: RequestTarget = {
        val ps: r.Params = testParams(r)
        RequestTarget(
          r.uriRaw(ps).toString,
          r.pathRaw(ps),
          r.queryRaw(ps).groupBy(_._1).map { case (k, v) => k -> v.flatMap(_._2) })
      }
      def version: String = ""
    }

  val reqs = LazyList.continually(LazyList(
    mkRequest(route1),
    mkRequest(route2),
    mkRequest(route3),
    mkRequest(route4),
    mkRequest(route5)
  )).flatten.iterator

  @inline def run(router: Router): String = {
    val request = reqs.next
    Await.result(
      router.handlerFor(request).collect { case a: EssentialAction => a }.get
        .apply(request).run().flatMap(_.body.consumeData).map(_.utf8String),
      Duration.Inf)
  }
}

@State(Scope.Thread)
class PlayBenchmark {
  import playHelper._

  @TearDown(Level.Trial) def teardown(): Unit = Await.result(actorSystem.terminate.map(_ => ()), Duration.Inf)

  @Benchmark def play: String = run(playService)
  @Benchmark def routing: String = run(routingService)
  @Benchmark def routingManual: String = run(routingManualService)
}
