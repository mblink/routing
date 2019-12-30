package routing

import routing.internal._

trait Show[A] {
  def show(a: A): String
}

object Show {
  implicit def optionalShowFromCatsShow[A, T[_]](implicit ev: GetTc[CatsShow, T], tc: T[A]): Show[A] = {
    import ev._

    new Show[A] {
      def show(a: A): String = tc.show(a)
    }
  }

  object syntax {
    implicit class ShowOps[A](val a: A)(implicit s: Show[A]) extends AnyVal {
      def show: String = s.show(a)
    }
  }
}

sealed trait CatsShow extends TcHolder {
  override type Tc[A] = cats.Show[A]
}

object CatsShow {
  implicit val get: GetTc[CatsShow, cats.Show] = {
    new GetTc[CatsShow, cats.Show] {
      override def equiv[A] = implicitly
    }
  }
}
