---
id: play
title: Play Framework
---

To use your routes in a project using the Play framework, add the following to your `build.sbt`, choosing the
correct minor version of Play that you're using:

```scala
libraryDependencies += @PLAY_LATEST_DEPENDENCY@
```

The currently supported versions of Play are:

@PLAY_SUPPORTED_VERSIONS@

Play handlers will be of the shape `Params => play.api.mvc.Handler`

First let's rebuild our example routes.

```scala mdoc
import routing._

val Login = Method.GET / "login"
val Hello = Method.GET / "hello" / pathVar[String]("name")
val BlogPost = Method.GET / "post" / pathVar[String]("slug") :? queryParam[Int]("id")
```

Then we can define our handlers.

```scala mdoc
import _root_.play.api.mvc.{ActionBuilder, AnyContent, Request, Results}
import routing.play._
import scala.concurrent.ExecutionContext.Implicits.global

val action = new ActionBuilder.IgnoringBody()

val handledLogin = Login.handle(_ => action(Results.Ok("Login page")))
val handledHello = Hello.handle(name => action(Results.Ok(s"Hello, $name")))
val handledBlogPost = BlogPost.handle { case (slug, id) =>
  action((req: Request[AnyContent]) => Results.Ok(s"Blog post with id: $id, slug: $slug found at ${req.uri}"))
}
```

Handled routes can be composed into a play `Router` by passing them to `Route.router`:

```scala mdoc
import _root_.play.api.routing.Router

val router1: Router = Route.router(
  handledLogin,
  handledHello,
  handledBlogPost
)
```

If you prefer, you can call `Router.from` with a partial function that matches on your `Route`s manually:

```scala mdoc
val router2: Router = Router.from {
  case Login(_) => action(Results.Ok("Login page"))
  case Hello(name) => action(Results.Ok(s"Hello, $name"))
  case BlogPost(slug, id) =>
    action(req => Results.Ok(s"Blog post with id: $id, slug: $slug found at ${req.uri}"))
}
```

You can confirm that routes are matched correctly by passing some test requests to the router:

```scala mdoc
import org.apache.pekko.actor.ActorSystem
import _root_.play.api.libs.typedmap.TypedMap
import _root_.play.api.mvc.{EssentialAction, Headers, RequestHeader}
import _root_.play.api.mvc.request.{RemoteConnection, RequestTarget}
import scala.concurrent.Await
import scala.concurrent.duration._

implicit val actorSystem: ActorSystem = ActorSystem.create()

def fakeRequest(u: ReverseUri): RequestHeader =
  new RequestHeader {
    def attrs: TypedMap = TypedMap.empty
    def connection: RemoteConnection = RemoteConnection("", false, None)
    def headers: Headers = Headers()
    def method: String = u.method.name
    def target: RequestTarget =
      RequestTarget(u.toString, u.path,
        u.query.groupBy(_._1).map { case (k, v) => k -> v.flatMap(_._2) })
    def version: String = ""
  }

def testRoute(router: Router, call: Call): String = {
  val request: RequestHeader = fakeRequest(call.uri)
  val handler: EssentialAction = router.handlerFor(request).collect { case a: EssentialAction => a }.get
  Await.result(handler(request).run().flatMap(_.body.consumeData).map(_.utf8String), 1.second)
}

testRoute(router1, Login())
testRoute(router1, Hello("world"))
testRoute(router1, BlogPost("my-slug", 1))

testRoute(router2, Login())
testRoute(router2, Hello("world"))
testRoute(router2, BlogPost("my-slug", 1))
```

You can also check that requests matching none of your routes are not handled by the router:

```scala mdoc
def unhandled(method: Method, path: String) =
  router1.handlerFor(fakeRequest(ReverseUri(method, path, Vector())))

unhandled(Method.GET, "/fake")

// Not handled by `Hello` because the method doesn't match
unhandled(Method.POST, "/hello/world")
```

```scala mdoc:invisible
actorSystem.terminate()
```
