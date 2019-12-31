package org.http4s
package routing
package builder

import cats.Show
import cats.syntax.semigroup._
import extractor.QueryExtractor
import org.http4s.dsl.impl.{+&, OptionalQueryParamDecoderMatcher, Path, QueryParamDecoderMatcher}
import part.QueryPart
import scala.reflect.runtime.universe.TypeTag

sealed trait QueryBuilderLP { self: Route0 =>
  protected def nextQS[A](
    key: String,
    mkQSPart: A => QueryPart,
    extract: QueryExtractor[A],
  )(implicit tt: TypeTag[A]): Route.Aux[Method, PathParams, (QueryParams, A), (Params, A)] = new Route[Method, (Params, A)] {
    type PathParams = self.PathParams
    type QueryParams = (self.QueryParams, A)

    def mkParams(pp: PathParams, qp: QueryParams): Params =
      (self.mkParams(pp, qp._1), qp._2)

    lazy val show = self.show |+| Route.shownQueryParam[A](key)

    lazy val method = self.method
    def pathParts(params: Params) = self.pathParts(params._1)
    def queryParts(params: Params) = self.queryParts(params._1) :+ mkQSPart(params._2)

    lazy val paramTpes = self.paramTpes :+ tt

    def matchPath(path: Path): Option[(Path, PathParams)] = self.matchPath(path)
    def matchQuery(params: QPMap): Option[(QPMap, QueryParams)] =
      self.matchQuery(params).flatMap { case (ps, qs) => ps match {
        case extract(a) +& rest => Some((rest, (qs, a)))
        case _ => None
      }}
  }

  def :?[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[A])): Route.Aux[Method, PathParams, (QueryParams, A), (Params, A)] { type Method = self.Method } =
    nextQS[A](t._1, a => QueryPart.inst((t._1, a)), new QueryParamDecoderMatcher(t._1) {})

  def &[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[A])): Route.Aux[Method, PathParams, (QueryParams, A), (Params, A)] { type Method = self.Method } =
    :?(t)
}

trait QueryBuilder extends QueryBuilderLP { self: Route0 =>
  def :?[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[Option[A]]))(implicit d: DummyImplicit): Route.Aux[Method, PathParams, (QueryParams, Option[A]), (Params, Option[A])] { type Method = self.Method } =
    nextQS[Option[A]](t._1, a => QueryPart.inst((t._1, a)), new OptionalQueryParamDecoderMatcher(t._1) {})

  def &[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[Option[A]]))(implicit d: DummyImplicit): Route.Aux[Method, PathParams, (QueryParams, Option[A]), (Params, Option[A])] { type Method = self.Method } =
    :?(t)
}
