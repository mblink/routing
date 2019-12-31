---
id: forward-routing
title: Forward Routing
---

You can use the routes you've built in your http4s application to match a request, extract the relevant parameters from it, and perform route-specific logic using those parameters. First we define some routes and an http4s service:

```scala mdoc
import cats.instances.int._
import cats.instances.string._
import org.http4s.dsl.io._
import org.http4s.routing._

val Login = GET / "login"
val Hello = GET / "hello" / ("name" -> StringVar)
val BlogPost = GET / "post" / ("slug" -> StringVar) :? queryParam[Int]("id")

val service = Route.httpRoutes(
  Login.handled(_ => Ok("Login page")),
  Hello.handled(name => Ok(s"Hello, $name")),
  BlogPost.handled { case (slug, id) => Ok(s"Blog post with id: $id, slug: $slug") }
)
```

Then we can confirm that routes are matched correctly by passing some test requests to the service:

```scala mdoc
import cats.effect.IO
import org.http4s.Request

def testRoute(call: Call) =
  service
    .run(Request[IO](method = call.route.method, uri = call.uri))
    .value
    .unsafeRunSync
    .get
    .as[String]
    .unsafeRunSync

testRoute(Login())
testRoute(Hello("world"))
testRoute(BlogPost("my-slug", 1))
```

We can also check that requests matching none of our routes are not handled by the service:

```scala mdoc
import org.http4s.{Method, Uri}

def unhandled(method: Method, path: String) =
  service
    .run(Request[IO](method = method, uri = Uri(path = path)))
    .value
    .unsafeRunSync

unhandled(GET, "/fake")

// Not handled by `Hello` because the method doesn't match
unhandled(POST, "/hello/world")
```
