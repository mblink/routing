package routing

import util.Nestable

trait Handled[+O] { self =>
  type M <: Method
  type FP
  type NP
  type R

  def route0: R
  def ev: R <:< Route[M, NP]
  final def route: Route[M, NP] = ev(route0)

  def nestable: Nestable[FP, Route[M, NP]#Params]
  def handle(params: FP): O
  final def handleNested(params: NP): O = handle(nestable.unnest(params))

  private def withHandler[O2](f: FP => O2): Handled.Aux[M, FP, NP, R, O2] = new Handled[O2] {
    type M = self.M
    type FP = self.FP
    type NP = self.NP
    type R = self.R

    lazy val route0 = self.route0
    lazy val ev = self.ev
    lazy val nestable = self.nestable
    def handle(params: FP): O2 = f(params)
  }

  def map[O2](f: O => O2): Handled.Aux[M, FP, NP, R, O2] =
    withHandler(fp => f(handle(fp)))
}

object Handled {
  type Aux[M_ <: Method, FP_, NP_, R_, O] = Handled[O] {
    type M = M_
    type FP = FP_
    type NP = NP_
    type R = R_
  }

  type For[R_, O] = Handled[O] { type R = R_ }

  trait Ops {
    implicit class RouteHandlerOps[R_](route: R_) { self =>
      sealed abstract class With[M_ <: Method, FP_, NP_](implicit ev0: R_ <:< Route[M_, NP_], n: Nestable[FP_, NP_]) {
        def with_[O](f: FP_ => O): Handled.Aux[M_, FP_, NP_, R_, O] = new Handled[O] {
          type M = M_
          type FP = FP_
          type NP = NP_
          type R = R_
          lazy val route0 = self.route
          lazy val ev = ev0
          lazy val nestable = n
          def handle(params: FP): O = f(params)
        }
      }

      def handle[M <: Method, FP, NP](implicit ev: R_ <:< Route[M, NP], n: Nestable[FP, NP]): With[M, FP, NP] =
        new With[M, FP, NP] {}
    }
  }
}
