package routing
package builder

trait QueryBuilder[M <: Method, P] { self: Route[M, P] =>
  def :?[A, V, PO](a: A)(implicit next: NextQuery[A, V, P, PO]): Route[Method, PO] =
    next(a, self)

  def &[A, V, PO](a: A)(implicit next: NextQuery[A, V, P, PO]): Route[Method, PO] =
    next(a, self)
}
