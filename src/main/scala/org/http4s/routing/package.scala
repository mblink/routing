package org.http4s

import scala.language.implicitConversions

package object routing extends routing.Handled.Ops {
  type QPMap = Map[String, collection.Seq[String]]

  implicit def methodToRoute[M <: Method](m: M): Route.Aux[M, Unit, Unit, Unit] = Route.empty(m)

  def root[M <: Method](m: M): Route.Aux[M, Unit, Unit, Unit] = Route.empty(m)

  def queryParam[A](key: String): (String, Option[A]) = key -> None
  def optionalQueryParam[A](key: String): (String, Option[Option[A]]) = key -> None
}
