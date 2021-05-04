package routing
package http4s

import org.{http4s => h}

private[http4s] trait SyntaxCompat {
  protected def queryToHttp4s(query: ReverseQuery): h.Query

  def uriToHttp4s(uri: ReverseUri): h.Uri =
    h.Uri(path = uri.path, query = queryToHttp4s(uri.query))
}
