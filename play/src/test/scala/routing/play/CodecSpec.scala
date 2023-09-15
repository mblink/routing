package routing
package play

import _root_.play.api.libs.typedmap.TypedMap
import _root_.play.api.mvc.{Headers, RequestHeader}
import _root_.play.api.mvc.request.{RemoteConnection, RequestTarget}
import routing.extractor.ExtractRequest

object CodecSpec extends routing.CodecSpec("play") {
  type URI = String
  type Request = RequestHeader

  def mkReq(u: ReverseUri): Request =
    new RequestHeader {
      def attrs: TypedMap = TypedMap.empty
      def connection: RemoteConnection = RemoteConnection("", false, None)
      def headers: Headers = Headers()
      def method: String = "GET"
      def target: RequestTarget = RequestTarget(u.toString, u.path,
        u.query.groupBy(_._1).map { case (k, v) => k -> v.flatMap(_._2) })
      def version: String = ""
    }
  implicit val extractRequest: ExtractRequest[Request] = playExtractRequest

  testPath()
  testQuery()
}
