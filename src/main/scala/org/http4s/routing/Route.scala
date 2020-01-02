package org.http4s
package routing

import builder._
import cats.{Applicative, Defer, Semigroup}
import cats.data.OptionT
import cats.instances.option._
import cats.syntax.traverse._
import org.http4s.dsl.impl.{:?, ->, Path, Root => DslRoot}
import part._
import util._
import scala.language.experimental.{macros => scalaMacros}
import scala.reflect.runtime.universe.TypeTag

abstract class Route0 extends PathBuilder with QueryBuilder { self =>
  type Method <: org.http4s.Method
  type PathParams
  type QueryParams
  type Params

  def mkParams(pp: PathParams, qp: QueryParams): Params

  def matchPath(path: Path): Option[(Path, PathParams)]
  def matchQuery(params: QPMap): Option[(QPMap, QueryParams)]

  def show: Route.Shown

  override final def toString: String = show.show

  def method: Method
  def pathParts(params: Params): Vector[PathPart]
  def queryParts(params: Params): Vector[QueryPart]

  def paramTpes: Vector[TypeTag[_]]

  def pathRaw(params: Params): Uri.Path = pathParts(params).map(_.show).mkString("/", "/", "")
  def path(params: Any*): Uri.Path = macro macros.ApplyNested.impl

  def queryRaw(params: Params): Query = Query(queryParts(params).flatMap(_.show):_*)
  def query(params: Any*): Query = macro macros.ApplyNested.impl

  def uriRaw(params: Params): Uri = Uri(path = pathRaw(params), query = queryRaw(params))
  def uri(params: Any*): Uri = macro macros.ApplyNested.impl

  def urlRaw(params: Params): Uri = uriRaw(params)
  def url(params: Any*): Uri = macro macros.ApplyNested.impl

  def call(params0: Params): Call = new Call {
    val route: self.type = self
    lazy val params = params0
  }

  def applyRaw(params: Params): Call = call(params)
  def apply(params: Any*): Call = macro macros.ApplyNested.impl

  def unapply0[F[_]](request: Request[F]): Option[Params] = {
    val m = method
    request match {
      case `m` -> p :? q => (matchPath(p), matchQuery(q)) match {
        // TODO - what's the correct behavior if there are any unmatched query params remaining?
        case (Some((DslRoot, pp)), Some((_, qp))) => Some(mkParams(pp, qp))
        case _ => None
      }
      case _ => None
    }
  }

  def unapply[F[_], A](req: Request[F])(implicit t: Nestable[A, Params]): Option[A] =
    unapply0(req).map(t.unnest(_))
}

abstract class Route[M <: Method, P] extends Route0 {
  type Method = M
  type Params = P
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
    def matchPath(path: Path): Option[(Path, PathParams)] = r.matchPath(path)
    def matchQuery(params: QPMap): Option[(QPMap, QueryParams)] = r.matchQuery(params)
    def method: Method = r.method
    def paramTpes: Vector[TypeTag[_]] = r.paramTpes
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

    def matchPath(path: Path): Option[(Path, Unit)] =
      Some((path, ()))

    def matchQuery(params: QPMap): Option[(QPMap, QueryParams)] =
      Some((params, ()))
  }

  trait MkHttpRoutes[F[_]] {
    def apply(routes: Handled[F]*)(
      implicit D: Defer[F],
      F: Applicative[F]
    ): HttpRoutes[F] =
      HttpRoutes[F](req => OptionT(routes.foldLeft(Option.empty[F[Response[F]]])((acc, r) =>
        acc.orElse(r.route.unapply0(req).map(x => r.handle(req)(r.nestable.unnest(x))))).sequence))
  }

  def httpRoutes[F[_]]: MkHttpRoutes[F] = new MkHttpRoutes[F] {}

  case class Shown(pathParts: Vector[String], queryParts: Vector[String]) {
    lazy val show: String = pathParts.mkString("/", "/", "") ++ (queryParts match {
      case Vector() => ""
      case _ => queryParts.mkString("?", "&", "")
    })
  }

  object Shown {
    implicit val semigroup: Semigroup[Shown] =
      Semigroup.instance((x, y) => Shown(x.pathParts ++ y.pathParts, x.queryParts ++ y.queryParts))
  }

  def shownPath[A](name: Either[String, String])(implicit tt: TypeTag[A]): Shown =
    Shown(Vector(name.fold(identity _, s => s"<$s: ${tt.tpe}>")), Vector())

  def shownQueryParam[A](key: String)(implicit tt: TypeTag[A]): Shown =
    Shown(Vector(), Vector(s"<$key: ${tt.tpe}>"))
}
