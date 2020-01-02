package org.http4s
package routing

import util.Nestable

trait Handled[F[_]] {
  type M <: Method
  type FP
  type NP
  type R

  def route0: R
  def ev: R <:< Route[M, NP]
  final def route: Route[M, NP] = ev(route0)
  def nestable: Nestable[FP, Route[M, NP]#Params]
  def handle: Request[F] => FP => F[Response[F]]
}

object Handled {
  type Aux[F[_], M_ <: Method, FP_, NP_, R_] = Handled[F] {
    type M = M_
    type FP = FP_
    type NP = NP_
    type R = R_
  }

  type For[F[_], R_] = Handled[F] {
    type R = R_
  }

  trait Ops {
    implicit class RouteHandlerOps[R_](route: R_) { self =>
      sealed abstract class With[M_ <: Method, FP_, NP_](implicit ev0: R_ <:< Route[M_, NP_], n: Nestable[FP_, NP_]) {
        def with_[F[_]](f: Request[F] => FP_ => F[Response[F]]): Handled.Aux[F, M_, FP_, NP_, R_] = new Handled[F] {
          type M = M_
          type FP = FP_
          type NP = NP_
          type R = R_
          lazy val route0 = self.route
          lazy val ev = ev0
          lazy val nestable = n
          lazy val handle = f
        }
      }

      def handle[M <: Method, FP, NP](implicit ev: R_ <:< Route[M, NP], n: Nestable[FP, NP]): With[M, FP, NP] =
        new With[M, FP, NP] {}
    }
  }
}
