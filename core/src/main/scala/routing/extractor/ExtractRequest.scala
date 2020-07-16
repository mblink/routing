package routing
package extractor

trait ExtractRequest[Request] {
  type ForwardPath
  type ForwardQuery

  def parts(request: Request): Option[(Method, ForwardPath, ForwardQuery)]
  def rootPath: RootPath[ForwardPath]
  def extractPath: ExtractPathPart[ForwardPath]
  def extractQuery: ExtractQueryPart[ForwardQuery]
}

object ExtractRequest {
  type Aux[R, P, Q] = ExtractRequest[R] {
    type ForwardPath = P
    type ForwardQuery = Q
  }
}
