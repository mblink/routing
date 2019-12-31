package org.http4s
package routing
package part

import cats.Show

sealed trait QueryPart extends UrlPart {
  def key: String
  def show: Option[(String, Option[String])]
}

trait QueryPartLP {
  protected def kv[A](t: (String, A))(implicit s: Show[A]): (String, Option[String]) =
    (t._1, Some(s.show(t._2)).filter(_.nonEmpty))

  protected def inst[A](t: (String, A), s: => Option[(String, Option[String])]): QueryPart.Aux[A] = new QueryPart {
    type T = A
    val key = t._1
    val value = t._2
    lazy val show = s
  }

  def inst[A](t: (String, A))(implicit s: Show[A]): QueryPart.Aux[A] = inst(t, Some(kv(t)))
}

object QueryPart extends QueryPartLP with UrlPart.Companion[QueryPart] {
  def inst[A](t: (String, Option[A]))(implicit s: Show[A], d: DummyImplicit): QueryPart.Aux[Option[A]] =
    inst(t, t._2.map(a => kv((t._1, a))))
}
