package routing
package part

import routing.util.Show
import routing.util.dummy._

sealed trait UrlPart {
  type T
  def value: T
}

object UrlPart {
  trait Companion[U <: UrlPart] { type Aux[T0] = U { type T = T0 } }
}

sealed trait PathPart extends UrlPart {
  def show: String
}

object PathPart extends UrlPart.Companion[PathPart] {
  sealed trait Single extends PathPart
  sealed trait Multi extends PathPart

  def single[A](a: A)(implicit s: Show[A]): Single { type T = A } = new Single {
    type T = A
    val value = a
    lazy val show = s.show(a)
  }

  def multi[A](a: A)(implicit s: Show[A]): Multi { type T = A } = new Multi {
    type T = A
    val value = a
    lazy val show = s.show(a)
  }
}

sealed trait QueryPart extends UrlPart {
  def key: String
  def show: Vector[(String, Option[String])]
}

sealed trait QueryPartBase {
  protected def kv[A](t: (String, A))(implicit s: Show[A]): (String, Option[String]) =
    (t._1, Some(s.show(t._2)).filter(_.nonEmpty))

  protected def inst[A](t: (String, A), s: => Vector[(String, Option[String])]): QueryPart.Aux[A] = new QueryPart {
    type T = A
    val key = t._1
    val value = t._2
    lazy val show = s
  }

  def inst[A](t: (String, A))(implicit s: Show[A]): QueryPart.Aux[A] = inst(t, Vector(kv(t)))
}

sealed trait QueryPartOptional extends QueryPartBase {
  def inst[A](t: (String, Option[A]))(implicit s: Show[A], @uu d: Dummy1): QueryPart.Aux[Option[A]] =
    inst(t, t._2.map(a => kv(t._1 -> a)).toVector)
}

sealed trait QueryPartMulti extends QueryPartOptional with UrlPart.Companion[QueryPart] {
  def inst[A](t: (String, List[A]))(implicit s: Show[A], @uu d: Dummy2): QueryPart.Aux[List[A]] =
    inst(t, t._2.map(a => kv(t._1 -> a)).toVector)
}

object QueryPart extends QueryPartMulti
