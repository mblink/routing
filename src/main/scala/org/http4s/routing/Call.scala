package org.http4s
package routing

trait Call {
  val route: Route0
  def params: route.Params

  final lazy val path: Uri.Path = route.path0(params)
  final lazy val queryString: Query = route.queryString0(params)
  final lazy val uri: Uri = route.uri0(params)
  final lazy val url: Uri = uri

  override def toString: String = uri.renderString
  final def asString: String = toString
}
