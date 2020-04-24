import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8

package object routing extends routing.Handled.Ops {
  type QueryMap = Map[String, collection.Seq[String]]

  type ReversePath = String
  type ReverseQuery = Vector[(String, Option[String])]

  implicit def methodToRoute[M <: Method](m: M): Route.Aux[M, Unit, Unit, Unit] = Route.empty(m)

  def root[M <: Method](m: M): Route.Aux[M, Unit, Unit, Unit] = Route.empty(m)

  def pathVar[A](name: String): (String, Option[A]) = name -> None

  case object RestOfPath
  def restOfPath(name: String): (String, RestOfPath.type) = name -> RestOfPath

  def queryParam[A](key: String): (String, Option[A]) = key -> None
  def multiQueryParam[A](key: String): (String, Option[List[A]]) = key -> None
  def optionalQueryParam[A](key: String): (String, Option[Option[A]]) = key -> None

  private[routing] val utf8 = UTF_8.name()

  private[routing] def queryUrlDecode(s: String): String = URLDecoder.decode(s, utf8)
  private[routing] def queryUrlEncode(s: String): String = URLEncoder.encode(s, utf8)

  private[routing] def pathUrlEncode(s: String): String = queryUrlEncode(s).replaceAllLiterally("+", "%20")
}
