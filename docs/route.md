---
id: route
title: Route
---

# [`Route`](../shared/ix/src/main/scala/bondlink/ix/Route.scala)

A `Route` represents sets of path parts and query string pairs, where:

  - A path part may be a `String` or a type `A` with typeclass instances providing the behavior:
    ```scala
    def apply(a: A): String
    def unapply(s: String): Option[A]
    ```
  - A query string pair is a tuple of a `String` and a type `A` with typeclass instances providing the behavior:
    ```scala
    // The `Option[String]` here is because a query param can be either /path?k=v or just /path?k
    def apply(kv: (String, Option[A])): Option[(String, Option[String])]
    def unapply(params: Map[String, Seq[String]]): Option[(String, Option[A])]
    ```
  - The product of all types `A` from above is the known type `Params`

Given these things, we can generically define functions to build a URL and to match on a URL:

```scala
def apply(params: Params): org.http4s.Uri
def unapply(req: Request[IO]): Option[Params]
```

## Building a `Route`

You can build a parameterized `Route` with a DSL-like syntax, using extractor values for the parameter type:

```scala mdoc
import org.http4s.routing._
import org.http4s.dsl.io.{GET, IntVar}
import cats.instances.int._

// Implies a `Show[Int]` for building URLs
// `IntVar` is an extractor, e.g. it conforms to
// `def unapply(s: String): Option[Int]` for parsing URLs
GET / "path" / ("id" -> IntVar)
```

Adding query params is simple as well:

```scala mdoc
import cats.instances.boolean._
import cats.instances.string._

// Implies a `Show[Boolean]` and `Show[String]`, as well as
// an `org.http4s.QueryParamDecoder` for each type
GET / "path" :? ("key1" -> Option.empty[Boolean]) & ("key2" -> Option.empty[String])
```

When building a `Route`, the `Params` type mentioned above is updated with every call to `/`, `?`, and `&`:

```scala mdoc
val root: Route { type Params = Unit } = GET
val path1: Route { type Params = Unit } = root / "path" // Static path parts add no parameters
val path2: Route { type Params = (Unit, Int) } = path1 / ("id" -> IntVar)
val path3: Route { type Params = ((Unit, Int), Boolean) } = path2 :? ("key1" -> Option.empty[Boolean])
val path4: Route { type Params = (((Unit, Int), Boolean), String) } = path3 & ("key2" -> Option.empty[String])
```

The `Params` type ends up as an increasingly nested `Tuple2`, but the [`Nestable` typeclass](./Nestable.md) lets us define functions that accept a flattened tuple instead:

```scala mdoc
// Using nested params
path4(((((), 1), true), "foo"))

// Using flat params
path4((1, true, "foo"))
```
