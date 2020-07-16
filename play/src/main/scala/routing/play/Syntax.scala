package routing
package play

import _root_.play.api.mvc.{Call => PlayCall, Handler => PlayHandler, RequestHeader => PlayRequest}
import _root_.play.api.routing.{Router => PlayRouter}
import routing.util.Show
import scala.annotation.tailrec

object syntax {
  implicit class PlayCallOps(val call: Call) extends AnyVal {
    def toPlay: PlayCall = PlayCall(call.route.method.name, Show(call.uri))
  }

  @tailrec
  private def tryRoutes(
    request: PlayRequest,
    handlers: List[Handled[PlayHandler]]
  ): Option[PlayHandler] =
    handlers match {
      case Nil => None
      case handler :: rest =>
        handler.route.unapplyNested(request) match {
          case Some(params) => Some(handler.handleNested(params))
          case None => tryRoutes(request, rest)
        }
    }

  trait MkRouter {
    def apply(handlers: Handled[PlayHandler]*): PlayRouter =
      PlayRouter.from(Function.unlift(tryRoutes(_: PlayRequest, handlers.toList)))
  }

  implicit class PlayRouteObjectOps(val route: Route.type) extends AnyVal {
    def router: MkRouter = new MkRouter {}
  }
}
