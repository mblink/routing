package routing

import routing.builder.{NextPath, NextQuery, NextRoute}
import routing.part.UrlPart

trait RouteF[PI, PO] { self =>
  def apply[M <: Method](r: Route[M, PI]): Route[M, PO]

  private def nextInst[A, V, POO](a: A)(implicit next: NextRoute[_ <: UrlPart, A, V, PO, POO]): RouteF[PI, POO] =
    new RouteF[PI, POO] { def apply[M <: Method](r: Route[M, PI]): Route[M, POO] = next(a, self(r)) }

  final def /[A, V, POO](a: A)(implicit next: NextPath[A, V, PO, POO]): RouteF[PI, POO] = nextInst(a)
  final def :?[A, V, POO](a: A)(implicit next: NextQuery[A, V, PO, POO]): RouteF[PI, POO] = nextInst(a)
  final def &[A, V, POO](a: A)(implicit next: NextQuery[A, V, PO, POO]): RouteF[PI, POO] = nextInst(a)
}

object RouteF {
  def id[P]: RouteF[P, P] = new RouteF[P, P] { def apply[M <: Method](r: Route[M, P]): Route[M, P] = r }
}
