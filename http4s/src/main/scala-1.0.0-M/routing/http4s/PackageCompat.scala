package routing
package http4s

import org.http4s.Uri.Path

private[http4s] trait PackageCompat {
  protected type Http4sPath = Path
  protected val rootHttp4sPath: Http4sPath = Path.Root
  protected def http4sPathIsEmpty(path: Http4sPath): Boolean = path.isEmpty
}
