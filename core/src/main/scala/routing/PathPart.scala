package routing
package part

import routing.util.Show

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

  def multi(s: String): Multi { type T = String } = new Multi {
    type T = String
    val value = s
    lazy val show = s
  }
}
