package org.http4s
package routing

import builder._
import cats.Semigroup
import org.http4s.dsl.impl.{:?, ->, Path, Root => DslRoot}
import part._
import util.Nestable
import scala.reflect.runtime.universe.TypeTag

abstract class Route extends PathBuilder with QueryStringBuilder { self =>
  type PathParams
  type QueryStringParams
  type Params

  protected def mkParams(pp: PathParams, qp: QueryStringParams): Params

  protected def matchPath(path: Path): Option[(Path, PathParams)]
  protected def matchQueryString(params: Map[String, collection.Seq[String]]): Option[(Map[String, collection.Seq[String]], QueryStringParams)]

  def show: Route.Shown

  override final def toString: String = show.show

  def pathParts(params: Params): Vector[PathPart]
  def queryStringParts(params: Params): Vector[QueryStringPart]

  def paramTpes: Vector[TypeTag[_]]

  final def path(params: Params): Uri.Path = pathParts(params).map(_.show).mkString("/", "/", "")
  final def path[A](params: A)(implicit t: Nestable[A, Params]): Uri.Path = path(t.nest(params))

  final def queryString(params: Params): Query = Query(queryStringParts(params).flatMap(_.show):_*)
  final def queryString[A](params: A)(implicit t: Nestable[A, Params]): Query = queryString(t.nest(params))

  final def apply(params: Params): Uri = Uri(path = path(params), query = queryString(params))
  final def apply[A](params: A)(implicit t: Nestable[A, Params]): Uri = apply(t.nest(params))

  final def unapply[F[_]](method: Method, request: Request[F]): Option[Params] =
    request match {
      case `method` -> p :? q => (matchPath(p), matchQueryString(q)) match {
        // TODO - what's the correct behavior if there are any unmatched query params remaining?
        case (Some((DslRoot, pp)), Some((_, qp))) => Some(mkParams(pp, qp))
        case _ => None
      }
      case _ => None
    }
}

object Route {
  type Parameterized[P] = Route { type Params = P }

  type Aux[PP, QP, P] = Parameterized[P] {
    type PathParams = PP
    type QueryStringParams = QP
  }

  lazy val empty: Aux[Unit, Unit, Unit] = new Route { self =>
    type PathParams = Unit
    type QueryStringParams = Unit
    type Params = Unit

    protected def mkParams(pp: Unit, qp: Unit): Unit = ()

    lazy val show = Shown(Vector(), Vector())

    def pathParts(u: Unit) = Vector()
    def queryStringParts(u: Unit) = Vector()

    lazy val paramTpes = Vector()

    protected def matchPath(path: Path): Option[(Path, Unit)] =
      Some((path, ()))

    protected def matchQueryString(params: Map[String, collection.Seq[String]]): Option[(Map[String, collection.Seq[String]], QueryStringParams)] =
      Some((params, ()))
  }

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
