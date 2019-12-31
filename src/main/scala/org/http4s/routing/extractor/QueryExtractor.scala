package org.http4s
package routing
package extractor

import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import scala.language.implicitConversions

sealed trait QueryExtractor[A] {
  def unapply(m: QPMap): Option[A]
}

object QueryExtractor {
  def inst[A](f: QPMap => Option[A]): QueryExtractor[A] = new QueryExtractor[A] {
    def unapply(m: QPMap) = f(m)
  }

  implicit def fromQueryParamDecoderMatcher[A](m: QueryParamDecoderMatcher[A]): QueryExtractor[A] =
    inst(m.unapply(_))

  implicit def fromOptionalQueryParamDecoderMatcher[A](m: OptionalQueryParamDecoderMatcher[A]): QueryExtractor[Option[A]] =
    inst(m.unapply(_))
}
