package org.http4s
package routing
package extractor

import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import scala.language.implicitConversions

sealed trait QueryStringExtractor[A] {
  def unapply(m: QueryParams): Option[A]
}

object QueryStringExtractor {
  def inst[A](f: QueryParams => Option[A]): QueryStringExtractor[A] = new QueryStringExtractor[A] {
    def unapply(m: QueryParams) = f(m)
  }

  implicit def fromQueryParamDecoderMatcher[A](m: QueryParamDecoderMatcher[A]): QueryStringExtractor[A] =
    inst(m.unapply(_))

  implicit def fromOptionalQueryParamDecoderMatcher[A](m: OptionalQueryParamDecoderMatcher[A]): QueryStringExtractor[Option[A]] =
    inst(m.unapply(_))
}
