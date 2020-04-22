---
id: routes
title: Routes
---

A [`Route`](@GITHUB_BLOB_URL@/core/src/main/scala/routing/Route.scala) is the set of a URL's path parts (i.e. the parts separated by `/`) and its query string pairs. It may have zero or more parameters, where the type of each parameter can be written to and parsed from a `String`. A path part is either a static `String` or a parameter, while a query string pair is a tuple of a `String` key and a parameter.

## Building routes

The most basic route is the root URL, `/`.

```scala mdoc
import routing._

val home = root(Method.GET)
```

More complex routes can be built using the provided DSL.

### Building the path

Static path parts can be specified as `String`s:

```scala mdoc
Method.GET / "part1" / "part2"
```

Path parameters can be specified by passing a type and a `String` key to the `pathVar` method:

```scala mdoc
val edit = Method.GET / "edit" / pathVar[Int]("id")
val hello = Method.GET / "hello" / pathVar[String]("name")
```

For a parameter of type `T`, you must have implicit `routing.util.Show[T]` and `routing.extractor.PathExtractor[T]`
instances defined. Instances for `String`, `Int`, `Long`, `Boolean`, and `UUID` are provided.

### Building the query string

Query parameters can be specified by passing a `String` key and a type parameter to the `queryParam` method:

```scala mdoc
val test = Method.GET / "path" :? queryParam[Boolean]("key1") & queryParam[String]("key2")
```

For a parameter of type `T`, you must have implicit `routing.util.Show[T]` and `routing.extractor.QueryExtractor[T]`
instances defined.

#### Optional query parameters

Query parameters with optional values, i.e. ones that may appear in the URL with or without a value, can be specified
using the `optionalQueryParam` method:

```scala mdoc
val routeWithQueryParam = Method.GET / "path" :? optionalQueryParam[String]("key")
routeWithQueryParam(Some("value"))
routeWithQueryParam(None)
```

#### Multi query parameters

Similarly, query parameters that may apperar in the URL zero or more times can be specified using the `multiQueryParam`
method:

```scala mdoc
val routeWithMultiParam = Method.GET / "path" :? multiQueryParam[Int]("id")
routeWithMultiParam(Nil)
routeWithMultiParam(List(1, 2))
```
