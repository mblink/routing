package routing
package util

import java.util.UUID

trait Show[A] {
  def show(a: A): String
}

private[routing] sealed trait CatsShow extends TCHolder {
  type TC[A] = cats.Show[A]
}

private[routing] object CatsShow {
  implicit val get: GetTC[CatsShow, cats.Show] = new GetTC[CatsShow, cats.Show](new CatsShow {}) {
    override def equiv[A] = implicitly
  }
}

trait LPShow {
  final implicit def showFromCatsShow[F[_], A](implicit ev: GetTC[CatsShow, F], F: F[A]): Show[A] = {
    import ev._
    Show.show(F.show(_))
  }
}

object Show extends LPShow {
  def apply[A](implicit s: Show[A]): Show[A] = s
  def apply[A](a: A)(implicit s: Show[A]): String = s.show(a)

  def show[A](f: A => String): Show[A] = new Show[A] {
    def show(a: A): String = f(a)
  }

  implicit val stringShow: Show[String] = show(identity)
  implicit val intShow: Show[Int] = show(_.toString)
  implicit val longShow: Show[Long] = show(_.toString)
  implicit val booleanShow: Show[Boolean] = show(_.toString)
  implicit val uuidShow: Show[UUID] = show(_.toString)
}
