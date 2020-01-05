import org.http4s.routing.Build._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val http4sRouting = project.in(file("."))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(testSettings)
  .settings(
    name := "http4s-routing",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      catsCore,
      http4sCore,
      http4sDsl
    )
  )

lazy val bench = project.in(file("bench"))
  .settings(commonSettings)
  .settings(name := "bench")
  .dependsOn(http4sRouting)
  .enablePlugins(JmhPlugin)

lazy val docs = project.in(file("http4s-routing-docs"))
  .settings(
    moduleName := "http4s-routing-docs",
    mdocExtraArguments += "--no-link-hygiene",
    mdocVariables := Map(
      "VERSION" -> (http4sRouting / version).value,
      "GITHUB_REPO_URL" -> githubRepoUrl,
      "GITHUB_SRC_URL" -> githubSrcUrl,
      "GITHUB_ROUTING_URL" -> s"$githubSrcUrl/org/http4s/routing"
    )
  )
  .dependsOn(http4sRouting)
  .enablePlugins(MdocPlugin, DocusaurusPlugin)

lazy val example = project.in(file("example"))
  .settings(commonSettings)
  .settings(
    name := "http4s-routing-example",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.12.3",
      "io.circe" %% "circe-generic" % "0.12.3",
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "org.slf4j" % "slf4j-simple" % "1.7.30"
    )
  )
  .dependsOn(http4sRouting)

lazy val githubRepoUrl = "https://github.com/mblink/http4s-routing"
lazy val githubSrcUrl = s"$githubRepoUrl/blob/master/src/main/scala"
