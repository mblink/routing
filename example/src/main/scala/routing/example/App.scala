package routing.example

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder

object App extends IOApp {
  import org.typelevel.log4cats.LoggerFactory
  import org.typelevel.log4cats.noop.NoOpFactory

  implicit val loggerFactory: LoggerFactory[IO] = NoOpFactory[IO]

  override def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder.default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(Controller.actions.orNotFound)
      .build
      .useForever
}
