package org.http4s

package object routing {
  val Root = Url.empty
  val IntVar = dsl.impl.IntVar
  val LongVar = dsl.impl.LongVar
  val UUIDVar = dsl.impl.UUIDVar
}
