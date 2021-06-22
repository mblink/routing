package routing

import scala.scalajs.js.URIUtils

object UrlEncoder {

  private[routing] def queryEncode(s: String): String = URIUtils.encodeURI(s)
  private[routing] def pathEncode(s: String): String = queryEncode(s).replace("+", "%20")

}
