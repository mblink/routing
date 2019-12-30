---
id: building-routes
title: Building Routes
---

## Path

We can use the provided DSL to build a `Route` with path parameters:

```scala mdoc
import org.http4s.routing._
import org.http4s.dsl.io._
import cats.instances.int._

val edit = GET / "edit" / ("id" -> IntVar)
```

The type `A` of each parameter (in this case `Int`) must have an implicit `cats.Show[A]` instance, and the value specified should conform to the shape:

```scala
def unapply(s: String): Option[A]
```

To accept any `String` as a parameter, we can write

```scala mdoc
import cats.instances.string._

val hello = GET / "hello" / ("name" -> StringVar)
```

## Query string

Adding query parameters is simple as well:

```scala mdoc
import cats.instances.boolean._
import cats.instances.string._

val test = GET / "path" :? queryParam[Boolean]("key1") & queryParam[String]("key2")
```

The type `A` of each parameter (in this case `Boolean` and `String`) must have implicit `cats.Show[A]` and `org.http4s.QueryParamDecoder[A]` instances.

## Building an http4s service

You can build an http4s service composed of many `Route`s by calling `Route.httpRoutes` and specifying the handler logic for each route, e.g.:

```scala mdoc
val service = Route.httpRoutes(
  edit.handled(id => Ok(s"Editing id $id")),
  hello.handled(name => Ok(s"Hello, $name")),
  test.handled { case (v1, v2) => Ok(s"First value: $v1, second value: $v2") }
)
```
