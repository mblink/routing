package routing
package part

import routing.util.Show

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

object QueryPart extends UrlPart.Companion[QueryPart] {
  private[routing] def inst[F[_], A](
    t: (String, F[A]),
    toV: F[A] => Vector[A]
  )(implicit s: Show[A]): Aux[F[A]] = new QueryPart {
    type T = F[A]
    val (key, value) = t
    lazy val show = toV(value).map(a => (key, Some(s.show(a)).filter(_.nonEmpty)))
  }
}
