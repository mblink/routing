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

abstract class Route0 extends PathBuilder with QueryStringBuilder { self =>
  type Method <: org.http4s.Method
  type PathParams
  type QueryStringParams
  type Params

  def mkParams(pp: PathParams, qp: QueryStringParams): Params

  def matchPath(path: Path): Option[(Path, PathParams)]
  def matchQueryString(params: QueryParams): Option[(QueryParams, QueryStringParams)]

  def show: Route.Shown

  override final def toString: String = show.show

  def method: Method
  def pathParts(params: Params): Vector[PathPart]
  def queryStringParts(params: Params): Vector[QueryStringPart]

  def paramTpes: Vector[TypeTag[_]]

  def path0(params: Params): Uri.Path = pathParts(params).map(_.show).mkString("/", "/", "")
  def path(params: Any*): Uri.Path = macro macros.ApplyNested.impl

  def queryString0(params: Params): Query = Query(queryStringParts(params).flatMap(_.show):_*)
  def queryString(params: Any*): Query = macro macros.ApplyNested.impl

  def uri0(params: Params): Uri = Uri(path = path0(params), query = queryString0(params))
  def uri(params: Any*): Query = macro macros.ApplyNested.impl

  def url0(params: Params): Uri = uri0(params)
  def url(params: Any*): Query = macro macros.ApplyNested.impl

  def call(params0: Params): Call = new Call {
    val route: self.type = self
    lazy val params = params0
  }

  def apply0(params: Params): Call = call(params)
  def apply(params: Any*): Call = macro macros.ApplyNested.impl

  def unapply0[F[_]](request: Request[F]): Option[Params] = {
    val m = method
    request match {
      case `m` -> p :? q => (matchPath(p), matchQueryString(q)) match {
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
    type QueryStringParams = QP
  }

  abstract class From[M <: Method, P](val r: Route[M, P]) extends Route[M, P] {
    type PathParams = r.PathParams
    type QueryStringParams = r.QueryStringParams

    def mkParams(pp: PathParams, qp: QueryStringParams): Params = r.mkParams(pp, qp)
    def matchPath(path: Path): Option[(Path, PathParams)] = r.matchPath(path)
    def matchQueryString(params: QueryParams): Option[(QueryParams, QueryStringParams)] = r.matchQueryString(params)
    def method: Method = r.method
    def paramTpes: Vector[TypeTag[_]] = r.paramTpes
    def pathParts(params: Params): Vector[PathPart] = r.pathParts(params)
    def queryStringParts(params: Params): Vector[QueryStringPart] = r.queryStringParts(params)
    def show: Shown = r.show
  }

  def empty[M <: Method](m: M): Aux[M, Unit, Unit, Unit] = new Route[M, Unit] { self =>
    type PathParams = Unit
    type QueryStringParams = Unit

    def mkParams(pp: Unit, qp: Unit): Unit = ()

    lazy val show = Shown(Vector(), Vector())

    lazy val method = m
    def pathParts(u: Unit) = Vector()
    def queryStringParts(u: Unit) = Vector()

    lazy val paramTpes = Vector()

    def matchPath(path: Path): Option[(Path, Unit)] =
      Some((path, ()))

    def matchQueryString(params: QueryParams): Option[(QueryParams, QueryStringParams)] =
      Some((params, ()))
  }

  abstract class HandledRoute[F[_], R <: Route0](val route: R) {
    def handle: Nestable[?, route.Params] /~\ (? => F[Response[F]])
  }

  trait MkHttpRoutes[F[_]] {
    def apply(routes: HandledRoute[F, _ <: Route0]*)(
      implicit D: Defer[F],
      F: Applicative[F]
    ): HttpRoutes[F] =
      HttpRoutes[F](req => OptionT(routes.foldLeft(Option.empty[F[Response[F]]]) { (acc, r) =>
        val h = r.handle
        acc.orElse(r.route.unapply0(req).map(x => h.b(h.a.unnest(x))))
      }.sequence))
  }

  def httpRoutes[F[_]]: MkHttpRoutes[F] = new MkHttpRoutes[F] {}

  case class Shown(pathParts: Vector[String], queryStringParts: Vector[String]) {
    lazy val show: String = pathParts.mkString("/", "/", "") ++ (queryStringParts match {
      case Vector() => ""
      case _ => queryStringParts.mkString("?", "&", "")
    })
  }

  object Shown {
    implicit val semigroup: Semigroup[Shown] =
      Semigroup.instance((x, y) => Shown(x.pathParts ++ y.pathParts, x.queryStringParts ++ y.queryStringParts))
  }

  def shownPath[A](name: Either[String, String])(implicit tt: TypeTag[A]): Shown =
    Shown(Vector(name.fold(identity _, s => s"<$s: ${tt.tpe}>")), Vector())

  def shownQueryParam[A](key: String)(implicit tt: TypeTag[A]): Shown =
    Shown(Vector(), Vector(s"<$key: ${tt.tpe}>"))
}
