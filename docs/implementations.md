---
id: implementations
title: Choosing an implementation
---

The routes we've built to this point are completely independent of any application framework. To wire them into an
HTTP application framework, we need to choose an implementation. Implementations are provided for
[http4s](https://github.com/http4s/http4s) and [Play Framework](https://github.com/playframework/playframework) and can
be added to your application with an extra dependency. Read on to see how to handle routes with each framework.
