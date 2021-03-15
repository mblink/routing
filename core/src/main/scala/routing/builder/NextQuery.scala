package routing
package builder

import izumi.reflect.Tag
import routing.extractor._
import routing.part._
import routing.util.{Show, Tupled}

trait NextQueryInstances {
  implicit def nextQueryParam[P, V, PO](
    implicit qe: QueryExtractor[V],
    s: Show[V],
    tt: Tag[V],
    tp: Tupled[P, V, PO]
  ): NextQuery[(String, Option[V]), V, P, PO] =
    new NextQuery[(String, Option[V]), V, P, PO] {
      def extract[ForwardPath, ForwardQuery](t: (String, Option[V]), path: ForwardPath, query: ForwardQuery)(
        implicit P: ExtractPathPart[ForwardPath],
        Q: ExtractQueryPart[ForwardQuery]
      ): Option[(V, ForwardPath, ForwardQuery)] =
        Q(query, t._1, qe).map { case (v, q) => (v, path, q) }
      def inputParams(params: PO): P = tp.untuple(params)._1
      def outputParams(params: P, v: V): PO = tp(params, v)
      def paramTpe: Option[Tag[_]] = Some(tt)
      def part(t: (String, Option[V]), params: PO): QueryPart = QueryPart.inst((t._1, tp.untuple(params)._2))
      def show(t: (String, Option[V])): Route.Shown = Route.shownPath[V](Right(t._1))
    }
}
