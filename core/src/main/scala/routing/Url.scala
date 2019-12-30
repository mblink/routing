package routing

import cats.effect.IO
import cats.instances.string._
import org.http4s.{Method, Request, Query, QueryParamDecoder, Uri}
import org.http4s.dsl.io.{:?, ->, /:, +&, OptionalQueryParamDecoderMatcher, Path, QueryParamDecoderMatcher, Root}
import scala.language.reflectiveCalls
import scala.reflect.runtime.universe.TypeTag
import Show.syntax._

sealed trait UrlPart {
  type T
  def value: T
}

object UrlPart {
  sealed trait Companion[U <: UrlPart] { type Aux[T0] = U { type T = T0 } }
}

sealed trait PathPart extends UrlPart {
  def show: String
}

object PathPart extends UrlPart.Companion[PathPart] {
  def inst[A](a: A)(implicit s: Show[A]): Aux[A] = new PathPart {
    type T = A
    val value = a
    lazy val show = s.show(a)
  }
}

sealed trait QueryStringPart extends UrlPart {
  def key: String
  def show: Option[(String, Option[String])]
}

trait QueryStringPartLP {
  protected def kv[A: Show](t: (String, A)): (String, Option[String]) =
    (t._1, Some(t._2.show).filter(_.nonEmpty))

  protected def inst[A](t: (String, A), s: => Option[(String, Option[String])]): QueryStringPart.Aux[A] = new QueryStringPart {
    type T = A
    val key = t._1
    val value = t._2
    lazy val show = s
  }

  def inst[A](t: (String, A))(implicit s: Show[A]): QueryStringPart.Aux[A] = inst(t, Some(kv(t)))
}

object QueryStringPart extends QueryStringPartLP with UrlPart.Companion[QueryStringPart] {
  def inst[A](t: (String, Option[A]))(implicit s: Show[A], d: DummyImplicit): QueryStringPart.Aux[Option[A]] =
    inst(t, t._2.map(a => kv((t._1, a))))
}

sealed trait QueryStringBuilderLP { self: Url =>
  protected type ExtractQS[A] = { def unapply(m: Map[String, Seq[String]]): Option[A] }

  protected def nextQS[A](
    mkQSPart: A => QueryStringPart,
    extract: ExtractQS[A],
  )(implicit tt: TypeTag[A]): Url.Aux[PathParams, (QueryStringParams, A), (Params, A)] = new Url {
    type PathParams = self.PathParams
    type QueryStringParams = (self.QueryStringParams, A)
    type Params = (self.Params, A)

    protected def mkParams(pp: PathParams, qp: QueryStringParams): Params =
      (self.mkParams(pp, qp._1), qp._2)

    def pathParts(params: Params) = self.pathParts(params._1)
    def queryStringParts(params: Params) = self.queryStringParts(params._1) :+ mkQSPart(params._2)

    lazy val paramTpes = self.paramTpes :+ tt

    protected def matchPath(path: Path): Option[(Path, PathParams)] = self.matchPath(path)
    protected def matchQueryString(params: Map[String, Seq[String]]): Option[(Map[String, Seq[String]], QueryStringParams)] =
      self.matchQueryString(params).flatMap { case (ps, qs) => ps match {
        case extract(a) +& rest => Some((rest, (qs, a)))
        case _ => None
      }}
  }

  def ?[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[A])): Url.Aux[PathParams, (QueryStringParams, A), (Params, A)] =
    nextQS[A](a => QueryStringPart.inst((t._1, a)), new QueryParamDecoderMatcher(t._1) {})

  def &[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[A])): Url.Aux[PathParams, (QueryStringParams, A), (Params, A)] =
    ?(t)
}

sealed trait QueryStringBuilder extends QueryStringBuilderLP { _: Url =>
  def ?[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[Option[A]]))(implicit d: DummyImplicit): Url.Aux[PathParams, (QueryStringParams, Option[A]), (Params, Option[A])] =
    nextQS[Option[A]](a => QueryStringPart.inst((t._1, a)), new OptionalQueryParamDecoderMatcher(t._1) {})

  def &[A: Show: TypeTag: QueryParamDecoder](t: (String, Option[Option[A]]))(implicit d: DummyImplicit): Url.Aux[PathParams, (QueryStringParams, Option[A]), (Params, Option[A])] =
    ?(t)
}

sealed trait PathBuilder { self: Url =>
  protected type ExtractPath[A] = { def unapply(s: String): Option[A] }

  private def nextPath[PP, P, A](
    getParams: P => Params,
    mkParams0: (PP, QueryStringParams) => P,
    mkPathPart: P => PathPart,
    extract: ExtractPath[A],
    mkNewParams: (PathParams, A) => PP,
    paramTpe: Option[TypeTag[_]]
  ): Url.Aux[PP, QueryStringParams, P] = new Url {
    type PathParams = PP
    type QueryStringParams = self.QueryStringParams
    type Params = P

    protected def mkParams(pp: PP, qp: QueryStringParams): P = mkParams0(pp, qp)

    def pathParts(params: P) = self.pathParts(getParams(params)) :+ mkPathPart(params)
    def queryStringParts(params: P) = self.queryStringParts(getParams(params))

    lazy val paramTpes = self.paramTpes ++ paramTpe

    protected def matchPath(path: Path): Option[(Path, PP)] =
      self.matchPath(path).flatMap { case (p, ps) => p match {
        case extract(a) /: rest => Some((rest, mkNewParams(ps, a)))
        case _ => None
      }}

    protected def matchQueryString(params: Map[String, Seq[String]]): Option[(Map[String, Seq[String]], QueryStringParams)] =
      self.matchQueryString(params)
  }

  def /(s: String): Url.Aux[PathParams, QueryStringParams, Params] =
    nextPath[PathParams, Params, String](
      identity _,
      self.mkParams(_, _),
      _ => PathPart.inst(s),
      new { def unapply(x: String): Option[String] = Some(x).filter(_ == s) },
      (pp, _) => pp,
      None)

  def /[K, V: Show](t: (K, ExtractPath[V]))(implicit tt: TypeTag[V]): Url.Aux[(PathParams, V), QueryStringParams, (Params, V)] =
    nextPath[(PathParams, V), (Params, V), V](
      _._1,
      (pp, qp) => (self.mkParams(pp._1, qp), pp._2),
      p => PathPart.inst((t._1, p._2))(Show.show(_._2.show)),
      t._2,
      (_, _),
      Some(tt))
}

sealed abstract class Url extends PathBuilder with QueryStringBuilder { self =>
  type PathParams
  type QueryStringParams
  type Params

  protected def mkParams(pp: PathParams, qp: QueryStringParams): Params

  protected def matchPath(path: Path): Option[(Path, PathParams)]
  protected def matchQueryString(params: Map[String, Seq[String]]): Option[(Map[String, Seq[String]], QueryStringParams)]

  def pathParts(params: Params): Vector[PathPart]
  def queryStringParts(params: Params): Vector[QueryStringPart]

  def paramTpes: Vector[TypeTag[_]]

  final def path(params: Params): Uri.Path = pathParts(params).map(_.show).mkString("/", "/", "")
  final def path[A](params: A)(implicit t: Nestable[A, Params]): Uri.Path = path(t.nest(params))

  final def queryString(params: Params): Query = Query(queryStringParts(params).flatMap(_.show):_*)
  final def queryString[A](params: A)(implicit t: Nestable[A, Params]): Query = queryString(t.nest(params))

  final def apply(params: Params): Uri = Uri(path = path(params), query = queryString(params))
  final def apply[A](params: A)(implicit t: Nestable[A, Params]): Uri = apply(t.nest(params))

  final def unapply(method: Method, request: Request[IO]): Option[Params] =
    request match {
      case `method` -> p :? q => (matchPath(p), matchQueryString(q)) match {
        // TODO - what's the correct behavior if there are any unmatched query params remaining?
        case (Some((Root, pp)), Some((_, qp))) => Some(mkParams(pp, qp))
        case _ => None
      }
      case _ => None
    }
}

object Url {
  type Parameterized[P] = Url { type Params = P }

  type Aux[PP, QP, P] = Parameterized[P] {
    type PathParams = PP
    type QueryStringParams = QP
  }

  lazy val empty: Aux[Unit, Unit, Unit] = new Url {
    type PathParams = Unit
    type QueryStringParams = Unit
    type Params = Unit

    protected def mkParams(pp: Unit, qp: Unit): Unit = ()

    def pathParts(@scalaz.unused u: Unit) = Vector()
    def queryStringParts(@scalaz.unused u: Unit) = Vector()

    lazy val paramTpes = Vector()

    protected def matchPath(path: Path): Option[(Path, Unit)] =
      Some((path, ()))

    protected def matchQueryString(params: Map[String, Seq[String]]): Option[(Map[String, Seq[String]], QueryStringParams)] =
      Some((params, ()))
  }
}
