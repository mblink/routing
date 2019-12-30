package org.http4s
package routing
package part

trait UrlPart {
  type T
  def value: T
}

object UrlPart {
  trait Companion[U <: UrlPart] { type Aux[T0] = U { type T = T0 } }
}
