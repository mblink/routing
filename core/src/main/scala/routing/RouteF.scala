package routing

import routing.builder.{NextPath, NextQuery}

trait RouteF[PI, PO] { self =>
  def apply[M <: Method](r: Route[M, PI]): Route[M, PO]

  def /[A, V, POO](a: A)(implicit next: NextPath[A, V, PO, POO]): RouteF[PI, POO] =
    new RouteF[PI, POO] { def apply[M <: Method](r: Route[M, PI]): Route[M, POO] = self(r) / a }

  def :?[A, V, POO](a: A)(implicit next: NextQuery[A, V, PO, POO]): RouteF[PI, POO] =
    new RouteF[PI, POO] { def apply[M <: Method](r: Route[M, PI]): Route[M, POO] = self(r) :? a }

  def &[A, V, POO](a: A)(implicit next: NextQuery[A, V, PO, POO]): RouteF[PI, POO] =
    new RouteF[PI, POO] { def apply[M <: Method](r: Route[M, PI]): Route[M, POO] = self(r) & a }
}

object RouteF {
  def id[P]: RouteF[P, P] = new RouteF[P, P] { def apply[M <: Method](r: Route[M, P]): Route[M, P] = r }
}
