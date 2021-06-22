package routing
package util

private[routing] final class DualEv[Ev1[_[_]], Ev2[_[_]], F[_]](implicit val ev1: Ev1[F], implicit val ev2: Ev2[F])

private[routing] object DualEv {
  @inline implicit def inst[Ev1[_[_]], Ev2[_[_]], F[_]](implicit ev1: Ev1[F], ev2: Ev2[F]): DualEv[Ev1, Ev2, F] =
    new DualEv[Ev1, Ev2, F]
}

private[routing] trait EvFromDualEv {
  private[routing] final implicit def ev1FromDualEv[Ev1[_[_]], Ev2[_[_]], F[_]](implicit d: DualEv[Ev1, Ev2, F]): Ev1[F] =
    d.ev1

  private[routing] final implicit def ev2FromDualEv[Ev1[_[_]], Ev2[_[_]], F[_]](implicit d: DualEv[Ev1, Ev2, F]): Ev2[F] =
    d.ev2
}
