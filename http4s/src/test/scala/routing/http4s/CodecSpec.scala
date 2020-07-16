package routing
package http4s

import cats.effect.IO
import org.http4s.Uri
import routing.extractor.ExtractRequest

object CodecSpec extends routing.CodecSpec("http4s") {
  type URI = Uri
  type Request = org.http4s.Request[IO]

  def mkReq(u: ReverseUri): Request = org.http4s.Request[IO](uri = u.toHttp4s)
  implicit val extractRequest: ExtractRequest[Request] = http4sExtractRequest

  testPath()
  testQuery()
}
