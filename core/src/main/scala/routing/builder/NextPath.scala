package routing
package builder

import izumi.reflect.Tag
import routing.extractor._
import routing.part._
import routing.util.{Show, Tupled}

trait NextPathInstances {
  implicit def nextPathString[P]: NextPath[String, String, P, P] =
    new NextPath[String, String, P, P] {
      def extract[ForwardPath, ForwardQuery](s: String, path: ForwardPath, query: ForwardQuery)(
        implicit P: ExtractPathPart[ForwardPath],
        Q: ExtractQueryPart[ForwardQuery]
      ): Option[(String, ForwardPath, ForwardQuery)] =
        P(path, PathExtractor.stringPathExtractor(Some(_).filter(_ == s))).map { case (s, p) => (s, p, query) }
      def inputParams(params: P): P = params
      def outputParams(params: P, s: String): P = params
      def paramTpe: Option[Tag[_]] = None
      def part(s: String, params: P): PathPart = PathPart.single(s)
      def show(s: String): Route.Shown = Route.shownPath[String](Left(s))
    }

  implicit def nextPathParam[P, V, PO](
    implicit s: Show[V],
    tt: Tag[V],
    pe: PathExtractor[V],
    tp: Tupled[P, V, PO]
  ): NextPath[(String, Option[V]), V, P, PO] =
    new NextPath[(String, Option[V]), V, P, PO] {
      def extract[ForwardPath, ForwardQuery](t: (String, Option[V]), path: ForwardPath, query: ForwardQuery)(
        implicit P: ExtractPathPart[ForwardPath],
        Q: ExtractQueryPart[ForwardQuery]
      ): Option[(V, ForwardPath, ForwardQuery)] =
        P(path, pe).map { case (v, p) => (v, p, query) }
      def inputParams(params: PO): P = tp.untuple(params)._1
      def outputParams(params: P, v: V): PO = tp(params, v)
      def paramTpe: Option[Tag[_]] = Some(tt)
      def part(t: (String, Option[V]), params: PO): PathPart =
        PathPart.single((t._1, tp.untuple(params)._2))(Show.show(x => s.show(x._2)))
      def show(t: (String, Option[V])): Route.Shown = Route.shownPath[V](Right(t._1))
    }

  implicit def nextPathRestOfPathParam[P, V, PO](
    implicit s: Show[V],
    tt: Tag[V],
    pe: RestOfPathExtractor[V],
    tp: Tupled[P, V, PO]
  ): NextPath[(String, RestOfPath[V]), V, P, PO] =
    new NextPath[(String, RestOfPath[V]), V, P, PO] {
      def extract[ForwardPath, ForwardQuery](t: (String, RestOfPath[V]), path: ForwardPath, query: ForwardQuery)(
        implicit P: ExtractPathPart[ForwardPath],
        Q: ExtractQueryPart[ForwardQuery]
      ): Option[(V, ForwardPath, ForwardQuery)] =
        P(path, pe).map((_, P.rootPath(), query))
      def inputParams(params: PO): P = tp.untuple(params)._1
      def outputParams(params: P, v: V): PO = tp(params, v)
      def paramTpe: Option[Tag[_]] = Some(tt)
      def part(t: (String, RestOfPath[V]), params: PO): PathPart =
        PathPart.multi((t._1, tp.untuple(params)._2))(Show.show(x => s.show(x._2)))
      def show(t: (String, RestOfPath[V])): Route.Shown = Route.shownPath[V](Right(t._1))
    }
}
