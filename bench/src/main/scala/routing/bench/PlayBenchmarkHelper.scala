package routing
package bench

import org.apache.pekko.actor.ActorSystem
import _root_.play.api.libs.typedmap.TypedMap
import _root_.play.api.mvc.{ActionBuilder, EssentialAction, Handler, Headers, RequestHeader, Results}
import _root_.play.api.mvc.request.{RemoteConnection, RequestTarget}
import _root_.play.api.routing.Router
import _root_.play.api.routing.sird._
import routing.play._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object PlayBenchmarkHelper extends BenchmarkHelper[RequestHeader, Handler, Router] {
  implicit val actorSystem: ActorSystem = ActorSystem.create()
  val action = new ActionBuilder.IgnoringBody()

  def actionRes(s: String): Handler = action(Results.Ok(s))

  val playService = Router.from {
    case GET(p"/route1") => route1Res()
    case GET(p"/route2/$param") => route2Res(param)
    case POST(p"/route3/${int(param)}") => route3Res(param)
    case GET(p"/route4/part" ? q"enabled=${bool(enabled)}") => route4Res(enabled)
    case POST(p"/$param/route5" ? q"id=${int(id)}") => route5Res(param, id)
  }

  def router(handlers: Handled[Handler]*): Router = Route.router(handlers:_*)
  def manualRouter(pf: PartialFunction[RequestHeader, Handler]): Router = Router.from(pf)

  def request(r: Route[_ <: Method, _]): RequestHeader =
    new RequestHeader {
      def attrs: TypedMap = TypedMap.empty
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

  def runReq(request: RequestHeader, router: Router): String =
    Await.result(
      router.handlerFor(request).collect { case a: EssentialAction => a }.get
        .apply(request).run().flatMap(_.body.consumeData).map(_.utf8String),
      Duration.Inf)
}
