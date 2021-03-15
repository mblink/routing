---
id: http4s-1.0.0-M10
title: http4s 1.0.0-M10
---

To use your routes in a project using http4s @HTTP4S_VERSION@, add the following to your `build.sbt`:

```scala
libraryDependencies += "bondlink" %% "routing-http4s_@HTTP4S_VERSION@" % "@VERSION@"
```

http4s handlers will be of the shape `Params => org.http4s.Request[F] => F[org.http4s.Response[F]]`

First let's rebuild our example routes.

```scala mdoc
import routing._

val Login = Method.GET / "login"
val Hello = Method.GET / "hello" / pathVar[String]("name")
val BlogPost = Method.GET / "post" / pathVar[String]("slug") :? queryParam[Int]("id")
```

Then we can define our handlers.

```scala mdoc
import cats.effect.IO
import org.http4s.{Request, Response}
import org.http4s.dsl.io._
import routing.http4s._

val handledLogin = Login.handle(_ => (_: Request[IO]) => Ok("Login page"))
val handledHello = Hello.handle(name => (_: Request[IO]) => Ok(s"Hello, $name"))
val handledBlogPost = BlogPost.handle { case (slug, id) => (req: Request[IO]) =>
  Ok(s"Blog post with id: $id, slug: $slug found at ${req.uri}")
}
```

Handled routes can be composed into an http4s service by passing them to `Route.httpRoutes`:

```scala mdoc
import cats.effect.IO
import org.http4s.HttpRoutes

val service1: HttpRoutes[IO] = Route.httpRoutes(
  handledLogin,
  handledHello,
  handledBlogPost
)
```

If you prefer, you can call `HttpRoutes.of` with a partial function that matches on your `Route`s manually:

```scala mdoc
val service2: HttpRoutes[IO] = HttpRoutes.of {
  case Login(_) => Ok("Login page")
  case Hello(name) => Ok(s"Hello, $name")
  case req @ BlogPost(slug, id) =>
    Ok(s"Blog post with id: $id, slug: $slug found at ${req.uri}")
}
```

You can confirm that routes are matched correctly by passing some test requests to the service:

```scala mdoc
import org.http4s.Request

def testRoute(service: HttpRoutes[IO], call: Call): String =
  service
    .run(Request[IO](method = call.method.toHttp4s, uri = call.uri.toHttp4s))
    .semiflatMap(_.as[String])
    .value
    .unsafeRunSync()
    .get

testRoute(service1, Login())
testRoute(service1, Hello("world"))
testRoute(service1, BlogPost("my-slug", 1))

testRoute(service2, Login())
testRoute(service2, Hello("world"))
testRoute(service2, BlogPost("my-slug", 1))
```

You can also check that requests matching none of your routes are not handled by the service:

```scala mdoc
import org.http4s.{Method, Uri}

def unhandled(method: Method, path: String) =
  service1.run(Request[IO](method = method, uri = Uri(path = Uri.Path.fromString(path)))).value.unsafeRunSync()

unhandled(GET, "/fake")

// Not handled by `Hello` because the method doesn't match
unhandled(POST, "/hello/world")
```
