---
id: routing
title: Routing
---

Once you've built your `Route`s, they can be used to route HTTP requests to specific application logic. Each route
contains enough information to match it against a request and extract the relevant parameters from the request's path
and query string, so all you need to do is specify the route-specific handling logic. Assuming you have the test routes:

```scala mdoc
import routing._

val Login = Method.GET / "login"
val Hello = Method.GET / "hello" / pathVar[String]("name")
val BlogPost = Method.GET / "post" / pathVar[String]("slug") :? queryParam[Int]("id")
```

Then you can specify the handling logic for a given route by calling `handle.with_`. The argument passed to `with_`
should be a function of the type `Params => Out` where `Params` is a tuple of the route's parameters and `Out` is any
type you want to target with your handler.

Here's how you could handle the routes above, simply returning a `String` for each route:

```scala mdoc
val handledLogin = Login.handle.with_(_ => "Login page")
val handledHello = Hello.handle.with_(name => s"Hello, $name")
val handledBlogPost = BlogPost.handle.with_ { case (slug, id) =>
  s"Blog post with id: $id, slug: $slug found"
}
```

Of course the actual output type of your handlers will be dictated by the HTTP application framework you've chosen.
See the [implementations documentation](implementations.md) for more examples.
