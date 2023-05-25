addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.9.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.11.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.12")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.6" )
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.3")

resolvers += "bondlink-maven-repo" at "https://raw.githubusercontent.com/mblink/maven-repo/main"
addSbtPlugin("bondlink" % "sbt-git-publish" % "0.0.5")
