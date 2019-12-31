package org.http4s
package routing

trait Call {
  val route: Route0
  def params: route.Params

  final lazy val path: Uri.Path = route.pathRaw(params)
  final lazy val queryString: Query = route.queryStringRaw(params)
  final lazy val uri: Uri = route.uriRaw(params)
  final lazy val url: Uri = uri

  override def toString: String = uri.renderString
  final def asString: String = toString
}
