package routing

import routing.util.Show

trait Call {
  type Params
  val route: Route[_ <: Method, Params]
  def params: Params

  final lazy val method: Method = route.method
  final lazy val path: ReversePath = route.pathRaw(params)
  final lazy val query: ReverseQuery = route.queryRaw(params)
  final lazy val uri: ReverseUri = route.uriRaw(params)
  final lazy val url: ReverseUri = uri

  override def toString: String = Show(uri)
  final def asString: String = toString
}

object Call {
  implicit val show: Show[Call] = Show.show(_.toString)
}
