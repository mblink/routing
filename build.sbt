import routing.Build._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val http4sRouting: Project = project.in(file("."))
    .settings(commonSettings)
    .settings(publishSettings)
    .settings(testSettings)
    .settings(libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      catsCore,
      http4sCore,
      http4sDsl
    ))
    .settings(Seq(name := "http4s-routing"))
