---
id: reverse-routing
title: Reverse Routing
---

You can convert the routes you've built to `Call`s for simple and type-safe references to routes in your application. First we define our routes:

```scala mdoc
import cats.instances.int._
import cats.instances.string._
import org.http4s.dsl.io._
import org.http4s.routing._

val Login = GET / "login"
val Hello = GET / "hello" / ("name" -> StringVar)
val BlogPost = GET / "post" / ("slug" -> StringVar) :? queryParam[Int]("id")
```

Then we can apply the necessary parameters to build the `Call` for each route:

```scala mdoc
Login()
Hello("world")
BlogPost("test-slug", 1)
```

We can convert a `Call` to an http4s `Uri`, produce a `String` representation of the route's URL, or access the route's path and query string directly:

```scala mdoc
val blogPost = BlogPost("test-slug", 1)

blogPost.uri
blogPost.toString
blogPost.path
blogPost.queryString
```
