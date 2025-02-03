package routing
package extractor

import java.util.UUID
import scala.util.Try

trait PathExtractor[A] { self =>
  def extract(path: String): Option[A]

  final def mapO[B](f: A => Option[B]): PathExtractor[B] =
    new PathExtractor[B] {
      def extract(path: String): Option[B] = self.extract(path).flatMap(f)
    }

  final def map[B](f: A => B): PathExtractor[B] = mapO(a => Some(f(a)))
}

trait RestOfPathExtractor[A] { self =>
  def extract(path: String): Option[A]

  final def mapO[B](f: A => Option[B]): RestOfPathExtractor[B] =
    new RestOfPathExtractor[B] {
      def extract(path: String): Option[B] = self.extract(path).flatMap(f)
    }

  final def map[B](f: A => B): RestOfPathExtractor[B] = mapO(a => Some(f(a)))
}

object PathExtractor {
  def apply[A](implicit p: PathExtractor[A]): PathExtractor[A] = p

  private def inst[A](fromStr: String => Try[A]): PathExtractor[A] = new PathExtractor[A]  {
    def extract(path: String): Option[A] = if (path.isEmpty) None else fromStr(path).toOption
  }

  def stringPathExtractor(f: String => Option[String]): PathExtractor[String] =
    new PathExtractor[String] {
      def extract(path: String): Option[String] = f(path)
    }

  implicit val stringPathExtractor: PathExtractor[String] = stringPathExtractor(Some(_).filter(_.nonEmpty))
  implicit val intPathExtractor: PathExtractor[Int] = inst(s => Try(s.toInt))
  implicit val longPathExtractor: PathExtractor[Long] = inst(s => Try(s.toLong))
  implicit val booleanPathExtractor: PathExtractor[Boolean] = inst(s => Try(s.toBoolean))
  implicit val uuidPathExtractor: PathExtractor[UUID] = inst(s => Try(UUID.fromString(s)))
}

trait RootPath[ForwardPath] {
  def apply(): ForwardPath
}

trait ExtractPathPart[ForwardPath] {
  val rootPath: RootPath[ForwardPath]
  def apply[A](path: ForwardPath, extract: PathExtractor[A]): Option[(A, ForwardPath)]
  def apply[A](path: ForwardPath, extract: RestOfPathExtractor[A]): Option[A]
}
