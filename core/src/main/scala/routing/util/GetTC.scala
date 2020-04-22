package routing
package util

private[routing] trait TCHolder {
  type TC[_]
}

private[routing] trait GetTC[Name <: TCHolder, ResultTC[A]] {
  implicit def equiv[A]: ResultTC[A] =:= Name#TC[A]

  object reverse {
    implicit final def eqReverse[A]: Name#TC[A] =:= ResultTC[A] = {
      equiv[A].asInstanceOf[Name#TC[A] =:= ResultTC[A]]
    }
  }
}
