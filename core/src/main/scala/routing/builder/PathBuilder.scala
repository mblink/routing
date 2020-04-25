package routing
package builder

import izumi.reflect.Tags.Tag
import routing.extractor._
import routing.part._
import routing.util.Show

trait PathBuilder[M <: Method, P] { self: Route[M, P] =>
  private def nextPath[PP, PS, A: Tag](
    name: Either[String, String],
    getParams: PS => Params,
    mkParams0: (PP, QueryParams) => PS,
    mkPathPart: PS => PathPart,
    extract: PathExtractor[A],
    mkNewParams: (PathParams, A) => PP,
    paramTpe: Option[Tag[_]]
  ): Route.Aux[Method, PP, QueryParams, PS] = new Route[Method, PS] {
    type PathParams = PP
    type QueryParams = self.QueryParams

    def mkParams(pp: PP, qp: QueryParams): PS = mkParams0(pp, qp)

    lazy val show = self.show |+| Route.shownPath[A](name)

    lazy val method = self.method
    def pathParts(params: PS): Vector[PathPart] = self.pathParts(getParams(params)) :+ mkPathPart(params)
    def queryParts(params: PS): Vector[QueryPart] = self.queryParts(getParams(params))

    lazy val paramTpes = self.paramTpes ++ paramTpe

    def matchPath[ForwardPath](path: ForwardPath)(
      implicit P: ExtractPathPart[ForwardPath]
    ): Option[(ForwardPath, PP)] =
      self.matchPath(path).flatMap { case (p, ps) =>
        P(p, extract) match {
          case Some((a, rest)) => Some((rest, mkNewParams(ps, a)))
          case _ => None
        }
      }

    def matchQuery[ForwardQuery](query: ForwardQuery)(
      implicit Q: ExtractQueryPart[ForwardQuery]
    ): Option[(ForwardQuery, QueryParams)] =
      self.matchQuery(query)
  }

  def /(s: String): Route.Aux[Method, PathParams, QueryParams, Params] { type Method = self.Method } =
    nextPath[PathParams, Params, String](
      Left(s),
      identity _,
      self.mkParams(_, _),
      _ => PathPart.single(s),
      PathExtractor.stringPathExtractor(Some(_).filter(_ == s)),
      (pp, _) => pp,
      None)

  def /[V: Show](t: (String, Option[V]))(implicit s: Show[V], tt: Tag[V], P: PathExtractor[V]): Route.Aux[Method, (PathParams, V), QueryParams, (Params, V)] { type Method = self.Method } =
    nextPath[(PathParams, V), (Params, V), V](
      Right(t._1),
      _._1,
      (pp, qp) => (self.mkParams(pp._1, qp), pp._2),
      p => PathPart.single((t._1, p._2))(Show.show(t => s.show(t._2))),
      P,
      (_, _),
      Some(tt))

  def */(t: (String, RestOfPath.type)): Route.Aux[Method, (PathParams, String), QueryParams, (Params, String)] { type Method = self.Method } =
    new Route[self.Method, (Params, String)] {
      type PathParams = (self.PathParams, String)
      type QueryParams = self.QueryParams

      def mkParams(pp: PathParams, qp: QueryParams): Params = (self.mkParams(pp._1, qp), pp._2)

      lazy val show = self.show |+| Route.shownPath[String](Right(t._1 + "*"))

      lazy val method = self.method
      def pathParts(params: Params): Vector[PathPart] = self.pathParts(params._1) :+ PathPart.multi(params._2)
      def queryParts(params: Params): Vector[QueryPart] = self.queryParts(params._1)

      lazy val paramTpes = self.paramTpes :+ Tag[String]

      def matchPath[ForwardPath](path: ForwardPath)(
        implicit P: ExtractPathPart[ForwardPath]
      ): Option[(ForwardPath, PathParams)] =
        self.matchPath(path).flatMap { case (p, ps) =>
          P.rest(p) match {
            case Some((s, rest)) => Some((rest, (ps, s)))
            case _ => None
          }
        }

      def matchQuery[ForwardQuery](query: ForwardQuery)(
        implicit Q: ExtractQueryPart[ForwardQuery]
      ): Option[(ForwardQuery, QueryParams)] =
        self.matchQuery(query)
    }
}
