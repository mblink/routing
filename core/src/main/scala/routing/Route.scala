package routing

import izumi.reflect.Tag
import routing.builder._
import routing.extractor._
import routing.part._
import routing.util._

abstract class Route[M <: Method, P]
extends RouteMethods[M, P]
with PathBuilder[M, P]
with QueryBuilder[M, P] { self =>
  final type Method = M
  final type Params = P

  def matchUri[ForwardPath, ForwardQuery](path: ForwardPath, query: ForwardQuery)(
    implicit P: ExtractPathPart[ForwardPath],
    Q: ExtractQueryPart[ForwardQuery]
  ): Option[(ForwardPath, ForwardQuery, Params)]

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

  def queryRaw(params: Params): ReverseQuery = queryParts(params).flatMap(_.show)

  def uriRaw(params: Params): ReverseUri = ReverseUri(method, pathRaw(params), queryRaw(params))

  def urlRaw(params: Params): ReverseUri = uriRaw(params)

  def callRaw(params0: Params): Call = new Call {
    type Params = self.Params
    val route: self.type = self
    lazy val params = params0
  }

  def applyRaw(params: Params): Call = callRaw(params)

  def unapply[Request](request: Request)(implicit R: ExtractRequest[Request]): Option[Params] = {
    val m = method
    R.parts(request) match {
      case Some((`m`, p, q)) =>
        val root = R.rootPath()
        matchUri(p, q)(R.extractPath, R.extractQuery).collect { case (`root`, _, ps) => ps }
      case _ => None
    }
  }

  def handle[O](f: Params => O): Handled.Aux[Method, Params, self.type, O] = new Handled[O] {
    type M = Method
    type P = Params
    type R = self.type
    val route = self
    def handle(params: P): O = f(params)
  }

  def withFallback(other: Route[Method, Params]): Route.WithFallback[Method, Params, self.type, other.type] =
    new Route.WithFallback[Method, Params, self.type, other.type](self, other) {}
}

object Route {
  abstract class Root[M <: Method](val method: M) extends Route[M, Unit] {
    type PathParams = Unit
    type QueryParams = Unit

    lazy val show = Shown(Vector(), Vector())

    def pathParts(u: Unit) = Vector()
    def queryParts(u: Unit) = Vector()

    lazy val paramTpes = Vector()

    def matchUri[ForwardPath, ForwardQuery](path: ForwardPath, query: ForwardQuery)(
      implicit P: ExtractPathPart[ForwardPath],
      Q: ExtractQueryPart[ForwardQuery]
    ): Option[(ForwardPath, ForwardQuery, Params)] =
      Some((path, query, ()))
  }

  def root[M <: Method](m: M): Route[M, Unit] = new Root[M](m) {}

  abstract class Iso[M <: Method, PI, PO](val r: Route[M, PI])(io: PI => PO, oi: PO => PI) extends Route[M, PO] {
    def matchUri[ForwardPath, ForwardQuery](path: ForwardPath, query: ForwardQuery)(
      implicit P: ExtractPathPart[ForwardPath],
      Q: ExtractQueryPart[ForwardQuery]
    ): Option[(ForwardPath, ForwardQuery, Params)] = r.matchUri(path, query).map { case (p, q, ps) => (p, q, io(ps)) }
    def method: Method = r.method
    def paramTpes: Vector[Tag[_]] = r.paramTpes
    def pathParts(params: Params): Vector[PathPart] = r.pathParts(oi(params))
    def queryParts(params: Params): Vector[QueryPart] = r.queryParts(oi(params))
    def show: Shown = r.show

    override def unapply[R](request: R)(implicit R: ExtractRequest[R]): Option[Params] =
      r.unapply(request).map(io)
  }

  abstract class From[M <: Method, P](r: Route[M, P]) extends Iso[M, P, P](r)(identity, identity)

  abstract class WithFallback[M <: Method, P, R1 <: Route[M, P], R2 <: Route[M, P]](val main: R1, val fallback: R2) extends Route[M, P] {
    def method: Method = main.method
    def paramTpes: Vector[Tag[_]] = main.paramTpes
    def pathParts(params: Params): Vector[PathPart] = main.pathParts(params)
    def queryParts(params: Params): Vector[QueryPart] = main.queryParts(params)
    def show: Shown = main.show

    def matchUri[ForwardPath, ForwardQuery](path: ForwardPath, query: ForwardQuery)(
      implicit P: ExtractPathPart[ForwardPath],
      Q: ExtractQueryPart[ForwardQuery]
    ): Option[(ForwardPath, ForwardQuery, Params)] =
      main.matchUri(path, query).orElse(fallback.matchUri(path, query))
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
