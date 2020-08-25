package routing
package extractor

import java.util.UUID
import scala.util.Try

trait PathExtractor[A] {
  def extract(path: String): Option[A]
}

// Dumb duplicate trait to force callers to acknowledge that the String they're
// extracting from is the rest of the path and may contain multiple path segments
trait RestOfPathExtractor[A] extends PathExtractor[A]

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
  def apply[A](path: ForwardPath, extract: PathExtractor[A]): Option[(A, ForwardPath)]
  def rest(path: ForwardPath): Option[(String, ForwardPath)]
}
