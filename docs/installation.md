---
id: installation
title: Installation
---

The `routing-core` package provides simple, type-safe, and framework-agnostic forward and reverse routing for HTTP
applications. To get started, add the following to your SBT configuration:

```scala
resolvers += "bondlink-maven-repo" at "https://raw.githubusercontent.com/mblink/maven-repo/main"
libraryDependencies += "bondlink" %% "routing-core" % "@VERSION@"
```

**Note:** Versions 4 and above are built for Scala 2.13.x and Scala 3, and require at least Java 11. The last version
to support Scala 2.12 and Java 8 is 3.2.1.
