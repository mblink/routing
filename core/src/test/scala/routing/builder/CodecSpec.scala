package routing

import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import org.scalacheck.Prop.{forAll, propBoolean}
import routing.extractor.ExtractRequest

abstract class CodecSpec(name: String) extends Properties(s"$name.codec") {
  def matches[A](tpe: String, expected: A, actual: A): Prop =
    (expected == actual) :| s"$tpe mismatch\n    expected: $expected\n    actual: $actual"

  val pathEncodedChars = Map(
    '$' -> "%24",
    '&' -> "%26",
    '+' -> "%2B",
    ',' -> "%2C",
    '/' -> "%2F",
    ':' -> "%3A",
    ';' -> "%3B",
    '=' -> "%3D",
    '?' -> "%3F",
    '@' -> "%40",
    ' ' -> "%20",
    '"' -> "%22",
    '<' -> "%3C",
    '>' -> "%3E",
    '#' -> "%23",
    '%' -> "%25",
    '{' -> "%7B",
    '}' -> "%7D",
    '|' -> "%7C",
    '\\' -> "%5C",
    '^' -> "%5E",
    '~' -> "%7E",
    '[' -> "%5B",
    ']' -> "%5D",
    '`' -> "%60"
  )

  val queryEncodedChars = pathEncodedChars + (' ' -> "+")

  case class Str(str: String, idx: Int) {
    def patch(s: String): String = str.patch(idx, s, 0)
    def patch(c: Char): String = patch(s"${c}")
  }
  object Str {
    implicit val arb: Arbitrary[Str] =
      Arbitrary(for {
        str <- Gen.alphaNumStr
        idx <- Gen.chooseNum(0, str.length)
      } yield Str(str, idx))
  }

  val pathRoute = Method.GET / pathVar[String]("x")
  val queryRoute = Method.GET :? queryParam[String]("x")

  type URI
  type Request
  def mkReq(u: ReverseUri): Request
  implicit def extractRequest: ExtractRequest[Request]

  def testDecode(route: Route[Method.GET.type, (Unit, String)])(
    mkUri: String => ReverseUri,
    str: Str,
    raw: Char,
    encoded: String
  ): Prop =
    matches(s"$raw decoding",
      Some(((), str.patch(raw))),
      route.unapply0(mkReq(mkUri(str.patch(encoded)))))

  def testEncode(route: Route[Method.GET.type, (Unit, String)])(
    mkExpectedUri: String => String,
    str: Str,
    raw: Char,
    encoded: String
  ): Prop =
    matches(s"$raw encoding",
      mkExpectedUri(str.patch(encoded)),
      route.uri(str.patch(raw)).toString)

  def testPath() =
    pathEncodedChars.foreach { case (raw, encoded) =>
      property(s"decodes '$raw' in path") = forAll((s: Str) =>
        testDecode(pathRoute)(x => ReverseUri(Method.GET, s"/$x", Vector()), s, raw, encoded))

      property(s"encodes '$raw' in path") = forAll((s: Str) =>
        testEncode(pathRoute)(s => s"/$s", s, raw, encoded))
    }

  def testQuery() =
    queryEncodedChars.foreach { case (raw, encoded) =>
      property(s"decodes '$raw' in query") = forAll((s: Str) =>
        testDecode(queryRoute)(x => ReverseUri(Method.GET, "/", Vector("x" -> Some(x))), s, raw, encoded))

      property(s"encodes '$raw' in query") = forAll((s: Str) =>
        testEncode(queryRoute)(s => s"?x=$s", s, raw, encoded))
    }
}
