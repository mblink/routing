package routing.example

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder // 0.22, 0.23, 1.0.0-M39
import scala.concurrent.ExecutionContext // 0.22

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](ExecutionContext.global) // 0.22
    BlazeServerBuilder[IO] // 0.23, 1.0.0-M39
      .bindHttp(8080, "localhost")
      .withHttpApp(Controller.actions.orNotFound)
      .serve
      .compile
      .drain
      .map(_ => ExitCode.Success)
}
