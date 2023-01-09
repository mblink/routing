package routing

import java.net.URLEncoder

object UrlEncoder {

  private[routing] def queryEncode(s: String): String = URLEncoder.encode(s, utf8)
  private[routing] def pathEncode(s: String): String = queryEncode(s).replace("+", "%20")

}
