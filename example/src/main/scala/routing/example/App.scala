package routing.example

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(Controller.actions.orNotFound)
      .serve
      .compile
      .drain
      .map(_ => ExitCode.Success)
}
