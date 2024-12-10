package routing.example

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder

object App extends IOApp {
  // ++ 1.0.0-M44
  import org.typelevel.log4cats.LoggerFactory
  import org.typelevel.log4cats.noop.NoOpFactory

  private implicit val loggerFactory: LoggerFactory[IO] = NoOpFactory[IO]
  // -- 1.0.0-M44

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(Controller.actions.orNotFound)
      .serve
      .compile
      .drain
      .map(_ => ExitCode.Success)
}
