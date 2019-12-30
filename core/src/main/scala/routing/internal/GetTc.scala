package routing.internal

trait GetTc[Name <: TcHolder, ResultTc[A]] {
  implicit def equiv[A]: ResultTc[A] =:= Name#Tc[A]

  object reverse {
    implicit final def eqReverse[A]: Name#Tc[A] =:= ResultTc[A] = {
      equiv[A].asInstanceOf[Name#Tc[A] =:= ResultTc[A]]
    }
  }
}

trait GetTc1[Name <: TcHolder1, ResultTc[F[_]]] {
  implicit def equiv[F[_]]: ResultTc[F] =:= Name#Tc[F]

  object reverse {
    implicit final def eqReverse[F[_]]: Name#Tc[F] =:= ResultTc[F] = {
      equiv[F].asInstanceOf[Name#Tc[F] =:= ResultTc[F]]
    }
  }
}

trait TcHolder {
  type Tc[_]
}

trait TcHolder1 {
  type Tc[_[_]]
}
