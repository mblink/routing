package routing

import routing.extractor._
import _root_.play.api.mvc.RequestHeader
import _root_.play.api.routing.sird.{PathExtractor => _, _}

package object play {
  implicit def playDestructuredRequest(r: RequestHeader): DestructuredRequest.Aux[RequestHeader, String, QueryMap] =
    new DestructuredRequest {
      type Request = RequestHeader
      type ForwardPath = String
      type ForwardQuery = QueryMap

      lazy val request = r
      lazy val parts = Method.fromString(r.method).map((_, r.path, r.queryString))
    }

  implicit val playRootPath: RootPath[String] =
    new RootPath[String] {
      def apply(): String = "/"
    }

  implicit val playExtractPathPart: ExtractPathPart[String] =
    new ExtractPathPart[String] {
      def apply[A](path: String, extract: PathExtractor[A]): Option[(A, String)] =
        path match {
          case p"/$s" => extract.extract(s).map(_ -> "/")
          case p"/$s/$rest*" => extract.extract(s).map(_ -> s"/$rest")
          case _ => None
        }

      def rest(path: String): Option[(String, String)] =
        path match {
          case p"/$rest*" => Some(rest -> "/")
          case _ => None
        }
    }

  implicit val playExtractQueryPart: ExtractQueryPart[QueryMap] =
    new ExtractQueryPart[QueryMap] {
      def apply[A](query: QueryMap, key: String, extract: QueryExtractor[A]): Option[(A, QueryMap)] =
        extract.extract(key, query).map(_ -> query)
    }

  implicit def toPlayCallOps(call: Call): syntax.PlayCallOps = new syntax.PlayCallOps(call)
  implicit def toPlayRouteObjectOps(route: Route.type): syntax.PlayRouteObjectOps = new syntax.PlayRouteObjectOps(route)
}
