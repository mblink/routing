package routing
package extractor

import java.util.UUID
import scala.annotation.tailrec
import scala.util.Try

trait QueryExtractor[A] { self =>
  def extract(key: String, query: QueryMap): Option[A]

  final def mapO[B](f: A => Option[B]): QueryExtractor[B] =
    new QueryExtractor[B] {
      def extract(key: String, query: QueryMap): Option[B] = self.extract(key, query).flatMap(f)
    }

  final def map[B](f: A => B): QueryExtractor[B] = mapO(a => Some(f(a)))
}

object QueryExtractor {
  def apply[A](implicit q: QueryExtractor[A]): QueryExtractor[A] = q

  private def inst[A](fromStr: String => Try[A]): QueryExtractor[A] = new QueryExtractor[A] {
    def extract(key: String, query: QueryMap): Option[A] =
      query.get(key).flatMap(_.headOption).flatMap(fromStr(_).toOption)
  }

  implicit val stringQueryExtractor: QueryExtractor[String] = inst(Try(_))
  implicit val intQueryExtractor: QueryExtractor[Int] = inst(s => Try(s.toInt))
  implicit val longQueryExtractor: QueryExtractor[Long] = inst(s => Try(s.toLong))
  implicit val booleanQueryExtractor: QueryExtractor[Boolean] = inst(s => Try(s.toBoolean))
  implicit val uuidQueryExtractor: QueryExtractor[UUID] = inst(s => Try(UUID.fromString(s)))

  implicit def optionalQueryExtractor[A](implicit q: QueryExtractor[A]): QueryExtractor[Option[A]] =
    new QueryExtractor[Option[A]] {
      def extract(key: String, query: QueryMap): Option[Option[A]] =
        query.get(key).flatMap(_.headOption) match {
          case Some(_) => q.extract(key, query).map(Some(_))
          case None => Some(None)
        }
    }

  implicit def multiQueryExtractor[A](implicit q: QueryExtractor[A]): QueryExtractor[List[A]] =
    new QueryExtractor[List[A]] {
      @tailrec
      private def go(acc: Option[List[A]], k: String, vs: List[String]): Option[List[A]] =
        (acc, vs) match {
          case (x, Nil) => x
          case (None, _) => None
          case (Some(x), h :: t) => q.extract(k, Map(k -> Seq(h))) match {
            case Some(y) => go(Some(y :: x), k, t)
            case None => None
          }
        }

      def extract(key: String, query: QueryMap): Option[List[A]] =
        query.get(key) match {
          case Some(vs) => go(Some(Nil), key, vs.toList)
          case None => Some(Nil)
        }
    }
}

trait ExtractQueryPart[ForwardQuery] {
  def apply[A](query: ForwardQuery, key: String, extract: QueryExtractor[A]): Option[(A, ForwardQuery)]
}
