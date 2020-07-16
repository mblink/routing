package routing
package util

abstract class Nestable[Flat, Nested](f: Flat => Nested, g: Nested => Flat) {
  final def nest(a: Flat): Nested = f(a)
  final def unnest(b: Nested): Flat = g(b)
}

object Nestable extends NestableInstances
