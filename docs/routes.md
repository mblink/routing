---
id: routes
title: Routes
---

A [`Route`](@GITHUB_ROUTING_URL@/Route.scala) is the set of a URL's path parts (i.e. the parts separated by `/`) and its query string pairs. It may have zero or more parameters, where the type of each parameter can be written to and parsed from a `String`. A path part is either a static `String` or a parameter, while a query string pair is a tuple of a `String` key and a parameter.

## Building routes

The most basic route is the root URL, `/`.

```scala mdoc
import org.http4s.routing._
import org.http4s.dsl.io._

val home = root(GET)
```

More complex routes can be built using the provided DSL, which should look similar to http4s' DSL.

### Building the path

Static path parts can be specified as `String`s:

```scala mdoc
GET / "part1" / "part2"
```

Path parameters can be specified with a `String` key and a reference to an extractor value for the parameter type:

```scala mdoc
import cats.instances.int._

val edit = GET / "edit" / ("id" -> IntVar)
```

For a parameter of type `T`, you must have an implicit `cats.Show[T]` instance, and the extractor value specified should implement `def unapply(s: String): Option[T]` as `IntVar` does.

*Note: You can use the provided `StringVar` value for `String` path parameters:*

```scala mdoc
import cats.instances.string._

val hello = GET / "hello" / ("name" -> StringVar)
```

### Building the query string

Query parameters can be specified by passing a `String` key and a type paramter to the `queryParam` method:

```scala mdoc
import cats.instances.boolean._
import cats.instances.string._

val test = GET / "path" :? queryParam[Boolean]("key1") & queryParam[String]("key2")
```

For a parameter of type `T`, you must have implicit `cats.Show[T]` and `org.http4s.QueryParamDecoder[T]` instances.

#### Optional query parameters

Sometimes it may be useful to have query parameters with optional values, i.e. ones that may appear in the URL either of the following ways:

```
/path?key=value
/path?key
```

Query parameters with an optional value, i.e. ones that may appear in the URL as  can be specified using the `optionalQueryParam` method:

```scala mdoc
val routeWithQueryParam = GET / "path" :? optionalQueryParam[String]("key")

routeWithQueryParam.uri(Some("value")).renderString
routeWithQueryParam.uri(None).renderString
```
