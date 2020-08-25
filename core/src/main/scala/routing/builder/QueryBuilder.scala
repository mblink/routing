package routing
package builder

import izumi.reflect.Tag
import routing.extractor._
import routing.part.QueryPart
import routing.util.Show
import routing.util.dummy._

sealed trait QueryBuilderLP[M <: Method, P] { self: Route[M, P] =>
  protected def nextQS[A](
    key: String,
    mkQSPart: A => QueryPart,
    extract: QueryExtractor[A],
  )(implicit tt: Tag[A]): Route.Aux[Method, PathParams, (QueryParams, A), (Params, A)] = new Route[Method, (Params, A)] {
    type PathParams = self.PathParams
    type QueryParams = (self.QueryParams, A)

    def mkParams(pp: PathParams, qp: QueryParams): Params =
      (self.mkParams(pp, qp._1), qp._2)

    lazy val show = self.show |+| Route.shownQueryParam[A](key)

    lazy val method = self.method
    def pathParts(params: Params) = self.pathParts(params._1)
    def queryParts(params: Params) = self.queryParts(params._1) :+ mkQSPart(params._2)

    lazy val paramTpes = self.paramTpes :+ tt

    def matchPath[ForwardPath](path: ForwardPath)(
      implicit P: ExtractPathPart[ForwardPath],
      R: RootPath[ForwardPath]
    ): Option[(ForwardPath, PathParams)] = self.matchPath(path)

    def matchQuery[ForwardQuery](params: ForwardQuery)(
      implicit Q: ExtractQueryPart[ForwardQuery]
    ): Option[(ForwardQuery, QueryParams)] =
      self.matchQuery(params).flatMap { case (ps, qs) =>
        Q(ps, key, extract) match {
          case Some((a, rest)) => Some((rest, (qs, a)))
          case _ => None
        }
      }
  }

  def :?[A: Show: Tag](t: (String, Option[A]))(implicit Q: QueryExtractor[A]): Route.Aux[Method, PathParams, (QueryParams, A), (Params, A)] { type Method = self.Method } =
    nextQS[A](t._1, a => QueryPart.inst((t._1, a)), Q)

  def &[A: Show: Tag: QueryExtractor](t: (String, Option[A])): Route.Aux[Method, PathParams, (QueryParams, A), (Params, A)] { type Method = self.Method } =
    :?(t)
}

trait QueryBuilder[M <: Method, P] extends QueryBuilderLP[M, P] { self: Route[M, P] =>
  def :?[A: Show: Tag](t: (String, Option[Option[A]]))(implicit Q: OptionalQueryExtractor[A]): Route.Aux[Method, PathParams, (QueryParams, Option[A]), (Params, Option[A])] { type Method = self.Method } =
    nextQS[Option[A]](t._1, a => QueryPart.inst((t._1, a)), Q)

  def &[A: Show: Tag: OptionalQueryExtractor](t: (String, Option[Option[A]]))(implicit @unused d: Dummy1): Route.Aux[Method, PathParams, (QueryParams, Option[A]), (Params, Option[A])] { type Method = self.Method } =
    :?(t)

  def :?[A: Show: Tag](t: (String, Option[List[A]]))(implicit Q: MultiQueryExtractor[A]): Route.Aux[Method, PathParams, (QueryParams, List[A]), (Params, List[A])] { type Method = self.Method } =
    nextQS[List[A]](t._1, a => QueryPart.inst((t._1, a)), Q)

  def &[A: Show: Tag](t: (String, Option[List[A]]))(implicit Q: MultiQueryExtractor[A]): Route.Aux[Method, PathParams, (QueryParams, List[A]), (Params, List[A])] { type Method = self.Method } =
    :?(t)
}
