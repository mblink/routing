package routing
package util

private[routing] trait TCHolder {
  type TC[_]
}

private[routing] abstract class GetTC[Name <: TCHolder, ResultTC[A]](private[routing] val name: Name) {
  private[routing] implicit def equiv[A]: ResultTC[A] =:= name.TC[A]

  object reverse {
    implicit final def eqReverse[A]: name.TC[A] =:= ResultTC[A] = {
      equiv[A].asInstanceOf[name.TC[A] =:= ResultTC[A]]
    }
  }
}
