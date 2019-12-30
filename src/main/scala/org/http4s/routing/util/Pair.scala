package org.http4s
package routing
package util

sealed abstract class /~\[A[_], B[_]] {
  type T
  def a: A[T]
  def b: B[T]

  @inline final def substL[A0[_]](implicit ev: A[T] <:< A0[T]): /~\.Aux[A0, B, T] = /~\[A0, B, T](ev(a), b)
  @inline final def substR[B0[_]](implicit ev: B[T] <:< B0[T]): /~\.Aux[A, B0, T] = /~\[A, B0, T](a, ev(b))

  @inline final def apply[O]()(implicit ev: B[T] <:< (A[T] => O)): O = ev(b)(a)
  @inline final def apply[O](implicit ev: B[T] <:< (A[T] => O), @unused d: DummyImplicit): O = ev(b)(a)
}

object /~\ {
  type APair[A[_], B[_]]  = A /~\ B
  type Aux[A[_], B[_], Z] = /~\[A, B] { type T = Z }

  type Fn0[F[_], G[_], A] = /~\.Aux[F, Lambda[a => G[a] => a], A]
  object Fn0 {
    def apply[F[_], G[_], A](fa: F[A], f: G[A] => A): Fn0[F, G, A] = /~\[F, Lambda[a => G[a] => a], A](fa, f)
  }

  type Fn[F[_], A] = /~\.Fn0[F, F, A]
  object Fn {
    def apply[F[_], A](fa: F[A], f: F[A] => A): Fn[F, A] = Fn0(fa, f)
  }

  @inline final def unapply[A[_], B[_]](p: A /~\ B): Some[(A[p.T], B[p.T])] =
    Some((p.a, p.b))

  @inline final def apply[A[_], B[_], Z](az: => A[Z], bz: => B[Z]): /~\.Aux[A, B, Z] =
    new /~\[A, B] {
      type T = Z
      def a: A[Z] = az
      def b: B[Z] = bz
    }
}
