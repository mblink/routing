package org.http4s
package routing
package builder

import cats.Show
import cats.syntax.semigroup._
import extractor.QueryStringExtractor
import org.http4s.dsl.impl.{+&, OptionalQueryParamDecoderMatcher, Path, QueryParamDecoderMatcher}
import part.QueryStringPart
import scala.reflect.runtime.universe.TypeTag

sealed trait QueryStringBuilderLP { self: Route0 =>
  protected def nextQS[A](
    key: String,
    mkQSPart: A => QueryStringPart,
    extract: QueryStringExtractor[A],
  )(implicit tt: TypeTag[A]): Route.Aux[Method, PathParams, (QueryStringParams, A), (Params, A)] = new Route[Method, (Params, A)] {
    type PathParams = self.PathParams
    type QueryStringParams = (self.QueryStringParams, A)

    def mkParams(pp: PathParams, qp: QueryStringParams): Params =
      (self.mkParams(pp, qp._1), qp._2)

    lazy val show = self.show |+| Route.shownQueryParam[A](key)

    lazy val method = self.method
    def pathParts(params: Params) = self.pathParts(params._1)
    def queryStringParts(params: Params) = self.queryStringParts(params._1) :+ mkQSPart(params._2)

    lazy val paramTpes = self.paramTpes :+ tt

    def matchPath(path: Path): Option[(Path, PathParams)] = self.matchPath(path)
    def matchQueryString(params: QueryParams): Option[(QueryParams, QueryStringParams)] =
      self.matchQueryString(params).flatMap { case (ps, qs) => ps match {
        case extract(a) +& rest => Some((rest, (qs, a)))
        case _ => None
      }}
  }

  def :?[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[A])): Route.Aux[Method, PathParams, (QueryStringParams, A), (Params, A)] { type Method = self.Method } =
    nextQS[A](t._1, a => QueryStringPart.inst((t._1, a)), new QueryParamDecoderMatcher(t._1) {})

  def &[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[A])): Route.Aux[Method, PathParams, (QueryStringParams, A), (Params, A)] { type Method = self.Method } =
    :?(t)
}

trait QueryStringBuilder extends QueryStringBuilderLP { self: Route0 =>
  def :?[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[Option[A]]))(implicit d: DummyImplicit): Route.Aux[Method, PathParams, (QueryStringParams, Option[A]), (Params, Option[A])] { type Method = self.Method } =
    nextQS[Option[A]](t._1, a => QueryStringPart.inst((t._1, a)), new OptionalQueryParamDecoderMatcher(t._1) {})

  def &[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[Option[A]]))(implicit d: DummyImplicit): Route.Aux[Method, PathParams, (QueryStringParams, Option[A]), (Params, Option[A])] { type Method = self.Method } =
    :?(t)
}
