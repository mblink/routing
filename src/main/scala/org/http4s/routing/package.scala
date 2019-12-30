package org.http4s

package object routing {
  val Root = Route.empty
  val IntVar = dsl.impl.IntVar
  val LongVar = dsl.impl.LongVar
  val UUIDVar = dsl.impl.UUIDVar
}
