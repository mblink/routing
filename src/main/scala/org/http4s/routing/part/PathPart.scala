package org.http4s
package routing
package part

import cats.Show

sealed trait PathPart extends UrlPart {
  def show: String
}

object PathPart extends UrlPart.Companion[PathPart] {
  def inst[A](a: A)(implicit s: Show[A]): Aux[A] = new PathPart {
    type T = A
    val value = a
    lazy val show = s.show(a)
  }
}
