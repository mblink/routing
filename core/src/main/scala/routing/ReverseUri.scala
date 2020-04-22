package routing

import routing.util.Show

final case class ReverseUri(method: Method, path: ReversePath, query: ReverseQuery) { self =>
  def asString: String = Show(self)
  override final def toString: String = asString
}

object ReverseUri {
  private def appendQueryPair(b: StringBuilder, t: (String, Option[String])): Unit = {
    b.append(urlEncode(t._1))
    t._2.foreach { v =>
      b.append('=')
      b.append(urlEncode(v))
    }
  }

  implicit val show: Show[ReverseUri] =
    Show.show { u =>
      val b = new StringBuilder("")

      if (u.path.nonEmpty && !u.path.startsWith("/")) {
        b.append('/')
        b.append(u.path)
      } else b.append(u.path)

      u.query match {
        case Vector() => ()
        case h +: rest =>
          b.append('?')
          appendQueryPair(b, h)
          rest.foreach { t =>
            b.append('&')
            appendQueryPair(b, t)
          }
      }

      b.toString
    }
}
