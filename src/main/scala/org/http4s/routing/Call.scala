package org.http4s
package routing

trait Call {
  val route: Route0
  def params: route.Params

  final lazy val path: Uri.Path = route.path(params)
  final lazy val queryString: Query = route.queryString(params)
  final lazy val uri: Uri = route.uri(params)
  final lazy val url: Uri = uri

  override def toString: String = uri.renderString
  final def asString: String = toString
}
