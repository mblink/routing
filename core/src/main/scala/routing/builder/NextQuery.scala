package routing
package builder

import izumi.reflect.Tag
import routing.extractor._
import routing.part._
import routing.util.{Show, Tupled}

trait NextQueryInstancesLP {
  protected def nextQueryInst[F[_], P, V, PO](toV: F[V] => Vector[V])(
    implicit qe: QueryExtractor[F[V]],
    s: Show[V],
    tt: Tag[F[V]],
    tp: Tupled[P, F[V], PO]
  ): NextQuery[(String, Option[F[V]]), F[V], P, PO] =
    new NextQuery[(String, Option[F[V]]), F[V], P, PO] {
      def extract[ForwardPath, ForwardQuery](t: (String, Option[F[V]]), path: ForwardPath, query: ForwardQuery)(
        implicit P: ExtractPathPart[ForwardPath],
        Q: ExtractQueryPart[ForwardQuery]
      ): Option[(F[V], ForwardPath, ForwardQuery)] =
        Q(query, t._1, qe).map { case (v, q) => (v, path, q) }
      def inputParams(params: PO): P = tp.untuple(params)._1
      def outputParams(params: P, v: F[V]): PO = tp(params, v)
      def paramTpe: Option[Tag[_]] = Some(tt)
      def part(t: (String, Option[F[V]]), params: PO): QueryPart = {
        val (k, fv) = (t._1, tp.untuple(params)._2)
        QueryPart.inst((k, fv), toV)
      }
      def show(t: (String, Option[F[V]])): Route.Shown = Route.shownPath[F[V]](Right(t._1))
    }

  implicit def nextQueryParamId[P, V, PO](
    implicit qe: QueryExtractor[V],
    s: Show[V],
    tt: Tag[V],
    tp: Tupled[P, V, PO]
  ): NextQuery[(String, Option[V]), V, P, PO] =
    nextQueryInst[Lambda[a => a], P, V, PO](Vector(_))
}

trait NextQueryInstances extends NextQueryInstancesLP {
  implicit def nextQueryParamOption[P, V, PO](
    implicit qe: QueryExtractor[Option[V]],
    s: Show[V],
    tt: Tag[Option[V]],
    tp: Tupled[P, Option[V], PO]
  ): NextQuery[(String, Option[Option[V]]), Option[V], P, PO] =
    nextQueryInst[Option, P, V, PO](_.toVector)

  implicit def nextQueryParamList[P, V, PO](
    implicit qe: QueryExtractor[List[V]],
    s: Show[V],
    tt: Tag[List[V]],
    tp: Tupled[P, List[V], PO]
  ): NextQuery[(String, Option[List[V]]), List[V], P, PO] =
    nextQueryInst[List, P, V, PO](_.toVector)
}
