package routing
package util

trait Tupled[A, B, O] {
  def tuple(a: A, b: B): O
  final def apply(a: A, b: B): O = tuple(a, b)

  def untuple(o: O): (A, B)
  final def unapply(o: O): Some[(A, B)] = Some(untuple(o))
}

object Tupled extends TupledInstances
