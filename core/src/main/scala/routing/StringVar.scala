package routing

object StringVar {
  def unapply(s: String): Option[String] = Some(s).filter(_.nonEmpty)
}
