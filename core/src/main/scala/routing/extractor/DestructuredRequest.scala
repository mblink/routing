package routing
package extractor

trait DestructuredRequest {
  type Request
  type ForwardPath
  type ForwardQuery

  def request: Request
  def parts: Option[(Method, ForwardPath, ForwardQuery)]
}

object DestructuredRequest {
  type Aux[R, P, Q] = DestructuredRequest {
    type Request = R
    type ForwardPath = P
    type ForwardQuery = Q
  }
}
