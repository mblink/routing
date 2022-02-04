import java.io.File
import routing.currentVersion
import routing.Build._
import scala.sys.process._

Global / onChangedBuildSource := ReloadOnSourceChanges

noPublishSettings

lazy val core = simpleProj(projectMatrix.in(file("core")), "core")
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      catsCore % Optional,
      izumiReflect
    ),
    Compile / sourceGenerators += Def.task {
      val generators = new File("git rev-parse --show-toplevel".!!.trim) / "generators"
      val srcManaged = (Compile / sourceManaged).value / "generated"

      def gen(scalaF: String, rubyF: String) = {
        println(s"Generating ${srcManaged / scalaF} with ${generators / rubyF}")
        IO.write(srcManaged / scalaF, Seq("ruby", (generators / rubyF).toString).!!)
        srcManaged / scalaF
      }

      Seq(
        gen("RouteMethods.scala", "route_methods.rb"),
        gen("TupledInstances.scala", "tupled_instances.rb"),
      )
    }
  )

lazy val http4s = http4sProj(projectMatrix.in(file("http4s")), "http4s")(axis => _ => _.settings(
  libraryDependencies ++= Seq(
    http4sDep("core", axis.version),
    http4sDep("dsl", axis.version),
    http4sDep("server", axis.version)
  )
))
  .settings(publishSettings)
  .settings(scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings")))
  .dependsOn(core % "compile->compile;test->test")

lazy val play = simpleProj(projectMatrix.in(file("play")), "play")
  .settings(publishSettings)
  .settings(libraryDependencies += playCore)
  .dependsOn(core % "compile->compile;test->test")

lazy val bench = http4sProj(projectMatrix.in(file("bench")), "bench")(_ => jsOrJvm => _.settings(
  scalacOptions ++= jsOrJvm.fold(_ => Seq("-P:scalajs:nowarnGlobalExecutionContext"), _ => Seq()),
))
  .settings(noPublishSettings)
  .dependsOn(core, http4s, play)
  .enablePlugins(JmhPlugin)

lazy val http4sImplFile = "http4s.md"

def http4sImplDoc(dir: File, axis: Http4sAxis.Value): (File, String) =
  (dir / "implementations" / http4sImplFile, s"${http4sImplFile.split(".md").head}-${axis.suffix}.md")

lazy val docs = http4sProj(projectMatrix.in(file("routing-docs")), "routing-docs")(axis => _.fold(
  _ => identity,
  _ => _.settings(
    mdocVariables ++= Map(
      "VERSION" -> currentVersion,
      "GITHUB_REPO_URL" -> githubRepoUrl,
      "GITHUB_BLOB_URL" -> s"$githubRepoUrl/blob/master",
      "HTTP4S_SUFFIX" -> axis.suffix,
      "HTTP4S_VERSION_COMMENT" -> axis.comment,
      "HTTP4S_PATH_CODE" -> (axis match {
        case Http4sAxis.v1_0_0_M10 => "Uri.Path.fromString(path)"
        case Http4sAxis.v0_22 |
             Http4sAxis.v0_23 |
             Http4sAxis.v1_0_0_M31 =>
          "Uri.Path.unsafeFromString(path)"
      }),
      "HTTP4S_UNSAFERUNSYNC_IMPORT" -> (axis match {
        case Http4sAxis.v0_23 | Http4sAxis.v1_0_0_M31 => "import cats.effect.unsafe.implicits.global\n"
        case _ => ""
      })
    )
  ).enablePlugins(MdocPlugin))
)
  .settings(noPublishSettings)
  .dependsOn(core, http4s, play)

lazy val buildDocsSite = taskKey[Unit]("Build GitHub pages documentation site locally")
lazy val publishDocsSite = taskKey[Unit]("Build and publish GitHub pages documentation site")

def runCmd(cmd: ProcessBuilder): Unit = {
  val code = cmd.!
  assert(code == 0, s"command returned $code: $cmd")
}

buildDocsSite := Def.taskDyn {
  val projs = Http4sAxis.all.map(axis => docs.finder(axis, VirtualAxis.jvm)(latestScalaV))
  val target = (ThisBuild / baseDirectory).value / "routing-docs" / "target"
  val out = target / "generated-site"
  Def.taskDyn {
    Def.sequential(projs.map(p => (p / mdoc).toTask(""))).map { _ =>
      IO.delete(out)
      IO.copyDirectory((projs.last / mdocOut).value, out)
      IO.copy(Http4sAxis.all.dropRight(1).map { axis =>
        val (srcFile, targetRelFile) = http4sImplDoc(target / s"${axis.suffix}-jvm-2.13" / "mdoc", axis)
        println(s"$srcFile -> ${new File(s"$out/implementations/$targetRelFile")}")
        srcFile -> new File(s"$out/implementations/$targetRelFile")
      })
      val website = (ThisBuild / baseDirectory).value / "website"
      runCmd(Process(Seq("npm", "install"), cwd = website) #&& Process(Seq("npm", "run", "build"), cwd = website))
    }
  }
}.value

publishDocsSite := Def.taskDyn {
  val gitUser = sys.env.getOrElse("GIT_USER", sys.error("Please set the `GIT_USER` environment variable to your GitHub username"))
  val useSsh = sys.env.getOrElse("USE_SSH", sys.error("Please set the `USE_SSH` environment variable to `true` or `false` " ++
                                                      "to specify whether connecting to GitHub should use SSH"))
  Def.sequential(
    buildDocsSite,
    Def.task(runCmd(Process(
      Seq("npm", "run", "publish-gh-pages"),
      (ThisBuild / baseDirectory).value / "website",
      "GIT_USER" -> gitUser,
      "USE_SSH" -> useSsh)))
  )
}.value

lazy val example = http4sProj(projectMatrix.in(file("example")), "example")(axis => jsOrJvm => _.settings(
  scalacOptions ++= jsOrJvm.fold(_ => Seq("-P:scalajs:nowarnGlobalExecutionContext"), _ => Seq()),
  libraryDependencies ++= Seq(
    http4sDep("circe", axis.version),
    http4sDep("blaze-server", axis.version)
  )
))
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "org.slf4j" % "slf4j-api" % "1.7.33",
      "org.slf4j" % "slf4j-simple" % "1.7.33"
    )
  )
  .dependsOn(core, http4s, play)

lazy val githubRepoUrl = "https://github.com/mblink/routing"
