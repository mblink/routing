package routing

import routing.util.Show

sealed abstract class Method(val name: String)
object Method {
  case object GET extends Method("GET")
  case object POST extends Method("POST")
  case object PUT extends Method("PUT")
  case object DELETE extends Method("DELETE")
  case object PATCH extends Method("PATCH")
  case object OPTIONS extends Method("OPTIONS")
  case object HEAD extends Method("HEAD")

  implicit val show: Show[Method] = Show.show(_.name)

  def fromString(name: String): Option[Method] =
    name.toUpperCase match {
      case "GET" => Some(GET)
      case "POST" => Some(POST)
      case "PUT" => Some(PUT)
      case "DELETE" => Some(DELETE)
      case "PATCH" => Some(PATCH)
      case "OPTIONS" => Some(OPTIONS)
      case "HEAD" => Some(HEAD)
      case _ => None
    }
}
