package org.http4s
package routing

object StringVar {
  def unapply(s: String): Option[String] = Some(s)
}
