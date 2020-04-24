package routing

import izumi.reflect.Tags.Tag
import routing.builder._
import routing.extractor._
import routing.part._
import routing.util._

abstract class Route[M <: Method, P]
extends PathBuilder[M, P]
with QueryBuilder[M, P] { self =>
  final type Method = M
  final type Params = P

  type PathParams
  type QueryParams

  def mkParams(pp: PathParams, qp: QueryParams): Params

  def matchPath[ForwardPath](path: ForwardPath)(
    implicit P: ExtractPathPart[ForwardPath]
  ): Option[(ForwardPath, PathParams)]

  def matchQuery[ForwardQuery](params: ForwardQuery)(
    implicit Q: ExtractQueryPart[ForwardQuery]
  ): Option[(ForwardQuery, QueryParams)]

  def show: Route.Shown

  override final def toString: String = show.show

  def method: Method
  def pathParts(params: Params): Vector[PathPart]
  def queryParts(params: Params): Vector[QueryPart]

  def paramTpes: Vector[Tag[_]]

  def pathRaw(params: Params): ReversePath = {
    val b = new StringBuilder("")
    pathParts(params).foreach { p =>
      b.append('/')
      p match {
        case m: PathPart.Multi => b.append(m.show.split('/').map(pathUrlEncode).mkString("/"))
        case s: PathPart.Single => b.append(pathUrlEncode(s.show))
      }
    }
    b.toString
  }
  def path(params: Any*): ReversePath = macro macros.ApplyNested.impl

  def queryRaw(params: Params): ReverseQuery = queryParts(params).flatMap(_.show)
  def query(params: Any*): ReverseQuery = macro macros.ApplyNested.impl

  def uriRaw(params: Params): ReverseUri = ReverseUri(method, pathRaw(params), queryRaw(params))
  def uri(params: Any*): ReverseUri = macro macros.ApplyNested.impl

  def urlRaw(params: Params): ReverseUri = uriRaw(params)
  def url(params: Any*): ReverseUri = macro macros.ApplyNested.impl

  def callRaw(params0: Params): Call = new Call {
    type Params = self.Params
    val route: self.type = self
    lazy val params = params0
  }
  def call(params: Any*): Call = macro macros.ApplyNested.impl

  def applyRaw(params: Params): Call = callRaw(params)
  def apply(params: Any*): Call = macro macros.ApplyNested.impl

  def unapply0[Request](request: Request)(implicit R: ExtractRequest[Request]): Option[Params] = {
    val m = method
    R.parts(request) match {
      case Some((`m`, p, q)) =>
        val root = R.rootPath()
        (matchPath(p)(R.extractPath), matchQuery(q)(R.extractQuery)) match {
          case (Some((`root`, pp)), Some((_, qp))) => Some(mkParams(pp, qp))
          case _ => None
        }
      case _ => None
    }
  }

  def unapply[Request, A](request: Request)(implicit R: ExtractRequest[Request], N: Nestable[A, Params]): Option[A] =
    unapply0(request).map(N.unnest(_))
}

object Route {
  type Aux[M <: Method, PP, QP, P] = Route[M, P] {
    type PathParams = PP
    type QueryParams = QP
  }

  abstract class From[M <: Method, P](val r: Route[M, P]) extends Route[M, P] {
    type PathParams = r.PathParams
    type QueryParams = r.QueryParams

    def mkParams(pp: PathParams, qp: QueryParams): Params = r.mkParams(pp, qp)
    def matchPath[ForwardPath](path: ForwardPath)(
      implicit P: ExtractPathPart[ForwardPath]
    ): Option[(ForwardPath, PathParams)] = r.matchPath(path)
    def matchQuery[ForwardQuery](params: ForwardQuery)(
      implicit Q: ExtractQueryPart[ForwardQuery]
    ): Option[(ForwardQuery, QueryParams)] = r.matchQuery(params)
    def method: Method = r.method
    def paramTpes: Vector[Tag[_]] = r.paramTpes
    def pathParts(params: Params): Vector[PathPart] = r.pathParts(params)
    def queryParts(params: Params): Vector[QueryPart] = r.queryParts(params)
    def show: Shown = r.show
  }

  def empty[M <: Method](m: M): Aux[M, Unit, Unit, Unit] = new Route[M, Unit] { self =>
    type PathParams = Unit
    type QueryParams = Unit

    def mkParams(pp: Unit, qp: Unit): Unit = ()

    lazy val show = Shown(Vector(), Vector())

    lazy val method = m
    def pathParts(u: Unit) = Vector()
    def queryParts(u: Unit) = Vector()

    lazy val paramTpes = Vector()

    def matchPath[ForwardPath](path: ForwardPath)(
      implicit P: ExtractPathPart[ForwardPath]
    ): Option[(ForwardPath, PathParams)] =
      Some((path, ()))

    def matchQuery[ForwardQuery](query: ForwardQuery)(
      implicit Q: ExtractQueryPart[ForwardQuery]
    ): Option[(ForwardQuery, QueryParams)] =
      Some((query, ()))
  }

  case class Shown(pathParts: Vector[String], queryParts: Vector[String]) { self =>
    lazy val show: String = pathParts.mkString("/", "/", "") ++ (queryParts match {
      case Vector() => ""
      case _ => queryParts.mkString("?", "&", "")
    })

    def |+|(other: Shown): Shown =
      Shown(self.pathParts ++ other.pathParts, self.queryParts ++ other.queryParts)
  }

  def shownPath[A](name: Either[String, String])(implicit tt: Tag[A]): Shown =
    Shown(Vector(name.fold(identity _, s => s"<$s: ${tt.tag}>")), Vector())

  def shownQueryParam[A](key: String)(implicit tt: Tag[A]): Shown =
    Shown(Vector(), Vector(s"<$key: ${tt.tag}>"))
}
