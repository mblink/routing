import java.io.File
import _root_.routing.Build._
import scala.sys.process._

Global / onChangedBuildSource := ReloadOnSourceChanges

noPublishSettings

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(testSettings)
  .settings(
    name := "routing-core",
    libraryDependencies ++= Seq(
      catsCore % Optional,
      izumiReflect
    ),
    sourceGenerators in Compile += Def.task {
      val generators = new File("git rev-parse --show-toplevel".!!.trim) / "generators"
      val srcManaged = (Compile / sourceManaged).value / "generated"

      def gen(scalaF: String, rubyF: String) = {
        println(s"Generating ${srcManaged / scalaF} with ${generators / rubyF}")
        IO.write(srcManaged / scalaF, Seq("ruby", (generators / rubyF).toString).!!)
        srcManaged / scalaF
      }

      Seq(
        gen("NestableInstances.scala", "nestable_instances.rb"),
        gen("RouteMethods.scala", "route_methods.rb")
      )
    }
  )

lazy val http4s = project.in(file("http4s"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(testSettings)
  .settings(
    name := "routing-http4s",
    libraryDependencies ++= Seq(
      http4sCore,
      http4sDsl
    )
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val play = project.in(file("play"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(testSettings)
  .settings(
    name := "routing-play",
    libraryDependencies += playCore
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val bench = project.in(file("bench"))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(name := "bench")
  .dependsOn(core, http4s, play)
  .enablePlugins(JmhPlugin)

lazy val docs = project.in(file("routing-docs"))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    moduleName := "routing-docs",
    mdocExtraArguments += "--no-link-hygiene",
    mdocVariables := Map(
      "VERSION" -> (core / version).value,
      "GITHUB_REPO_URL" -> githubRepoUrl,
      "GITHUB_BLOB_URL" -> s"$githubRepoUrl/blob/master"
    )
  )
  .dependsOn(core, http4s, play)
  .enablePlugins(MdocPlugin, DocusaurusPlugin)

lazy val example = project.in(file("example"))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    name := "routing-example",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.12.3",
      "io.circe" %% "circe-generic" % "0.12.3",
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "org.slf4j" % "slf4j-simple" % "1.7.30"
    )
  )
  .dependsOn(core, http4s, play)

lazy val githubRepoUrl = "https://github.com/mblink/routing"
