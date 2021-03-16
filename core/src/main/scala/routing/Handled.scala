package routing

trait Handled[+O] { self =>
  type M <: Method
  type P
  type R <: Route[M, P]

  val route: R
  def handle(params: route.Params): O

  private def withHandler[O2](f: P => O2): Handled.Aux[M, P, R, O2] = new Handled[O2] {
    type M = self.M
    type P = self.P
    type R = self.R

    val route = self.route
    def handle(params: route.Params): O2 = f(params)
  }

  def map[O2](f: O => O2): Handled.Aux[M, P, R, O2] =
    withHandler(fp => f(handle(fp)))
}

object Handled {
  type Aux[M_ <: Method, P_, R_ <: Route[M_, P_], O] = Handled[O] {
    type M = M_
    type P = P_
    type R = R_
  }

  type For[R_ <: Route[_ <: Method, _], O] = Handled[O] { type R = R_ }
}
