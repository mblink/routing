package routing
package builder

import routing.extractor._
import routing.part._

trait NextRoute[T <: UrlPart, A, V, PI, PO] { next =>
  def extract[ForwardPath, ForwardQuery](a: A, path: ForwardPath, query: ForwardQuery)(
    implicit P: ExtractPathPart[ForwardPath],
    Q: ExtractQueryPart[ForwardQuery]
  ): Option[(V, ForwardPath, ForwardQuery)]
  def inputParams(params: PO): PI
  def outputParams(params: PI, v: V): PO
  def component(a: A): Component
  def part(a: A, params: PO): T
  def show(a: A): Route.Shown

  final def apply[M <: Method](a: A, route: Route[M, PI]): Route[M, PO] =
    new Route[M, PO] {
      lazy val method = route.method
      lazy val components = route.components :+ next.component(a)
      lazy val show = route.show |+| next.show(a)

      def pathParts(params: Params): Vector[PathPart] =
        route.pathParts(next.inputParams(params)) ++ ((next.part(a, params): UrlPart) match {
          case p: PathPart => Vector(p)
          case _: QueryPart => Vector()
        })

      def queryParts(params: Params): Vector[QueryPart] =
        route.queryParts(next.inputParams(params)) ++ ((next.part(a, params): UrlPart) match {
          case q: QueryPart => Vector(q)
          case _: PathPart => Vector()
        })

      def matchUri[ForwardPath, ForwardQuery](path: ForwardPath, query: ForwardQuery)(
        implicit P: ExtractPathPart[ForwardPath],
        Q: ExtractQueryPart[ForwardQuery]
      ): Option[(ForwardPath, ForwardQuery, Params)] =
        route.matchUri(path, query).flatMap { case (newPath, newQuery, params) =>
          next.extract(a, newPath, newQuery).map { case (v, p, q) => (p, q, next.outputParams(params, v)) }
        }
    }
}

object NextRoute extends NextPathInstances with NextQueryInstances
