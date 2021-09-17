package routing.example

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder // 0.22, 0.23, 1.0.0-M25
import org.http4s.server.blaze.BlazeServerBuilder // 1.0.0-M10
import org.http4s.syntax.kleisli._ // 1.0.0-M10
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
