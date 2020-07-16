package routing.example

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import scala.concurrent.ExecutionContext

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(8080, "localhost")
      .withHttpApp(Controller.actions.orNotFound)
      .serve
      .compile
      .drain
      .map(_ => ExitCode.Success)
}
