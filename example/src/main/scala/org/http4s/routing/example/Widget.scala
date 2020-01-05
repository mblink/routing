package org.http4s.routing.example

import cats.effect.IO
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.http4s.EntityDecoder
import org.http4s.circe._

case class Widget(name: String)
object Widget {
  implicit val decoder: Decoder[Widget] = deriveDecoder
  implicit val encoder: Encoder[Widget] = deriveEncoder
  implicit val entityDecoder: EntityDecoder[IO, Widget] = jsonOf[IO, Widget]
}
