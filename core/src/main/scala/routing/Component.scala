package routing

import izumi.reflect.Tag

sealed trait Component
object Component {
  case class StaticPathPart(part: String) extends Component

  sealed trait Param extends Component {
    val name: String
    val tpe: Tag[_]
  }
  case class PathParam(name: String, tpe: Tag[_], multi: Boolean) extends Param
  case class QueryParam(name: String, tpe: Tag[_]) extends Param
}
