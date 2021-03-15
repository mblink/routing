package routing
package builder

case class RestOfPath[A]()

trait PathBuilder[M <: Method, P] { self: Route[M, P] =>
  def /[A, V, PO](a: A)(implicit next: NextPath[A, V, P, PO]): Route[Method, PO] = next(a, self)
}
