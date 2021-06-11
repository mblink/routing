import java.nio.charset.StandardCharsets.UTF_8

package object routing {
  type QueryMap = Map[String, collection.Seq[String]]

  type ReversePath = String
  type ReverseQuery = Vector[(String, Option[String])]

  implicit def methodToRoute[M <: Method](m: M): Route[M, Unit] = Route.root(m)

  def root[M <: Method](m: M): Route[M, Unit] = Route.root(m)

  def pathVar[A](name: String): (String, Option[A]) = name -> None

  def restOfPath[A](name: String): (String, builder.RestOfPath[A]) = name -> builder.RestOfPath()

  def queryParam[A](key: String): (String, Option[A]) = key -> None
  def multiQueryParam[A](key: String): (String, Option[List[A]]) = key -> None
  def optionalQueryParam[A](key: String): (String, Option[Option[A]]) = key -> None

  private[routing] val utf8 = UTF_8.name()
}
