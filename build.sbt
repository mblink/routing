import java.io.File
import routing.currentVersion
import routing.Build._
import scala.sys.process._

Global / onChangedBuildSource := ReloadOnSourceChanges

noPublishSettings

lazy val core = simpleProj(projectMatrix.in(file("core")), "core", List(
  Platform.Jvm,
  Platform.Js,
  Platform.Native,
))
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      catsCore.value % Optional,
      izumiReflect.value,
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
    http4sDep("core", axis.version).value,
    http4sDep("dsl", axis.version).value,
  )
))
  .settings(publishSettings)
  .settings(scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings")))
  .dependsOn(core % "compile->compile;test->test")

lazy val play = playProj(projectMatrix.in(file("play")), "play")(axis => _ => _.settings(
  libraryDependencies ++= Seq(axis.dep("play").value),
))
  .settings(publishSettings)
  .dependsOn(core % "compile->compile;test->test")

lazy val bench = http4sProj(projectMatrix.in(file("bench")), "bench", _ => List(Platform.Jvm))(
  _ => sjsNowarnGlobalECSettings,
)
  .settings(noPublishSettings)
  .dependsOn(core, http4s, play)
  .enablePlugins(JmhPlugin)

lazy val http4sImplFile = "http4s.md"

def http4sImplDoc(dir: File, axis: Http4sAxis.Value): (File, String) =
  (dir / "implementations" / http4sImplFile, s"${http4sImplFile.split(".md").head}-${axis.suffix}.md")

def playDepString(axis: PlayAxis.Value): String =
  s""""bondlink" %% "routing-play_${axis.suffix}" % "$currentVersion""""

lazy val docs = http4sProj(projectMatrix.in(file("routing-docs")), "routing-docs", _ => List(Platform.Jvm))(
  axis => _ => _.settings(
    scalacOptions -= "-Xfatal-warnings",
    mdocVariables ++= Map(
      "VERSION" -> currentVersion,
      "GITHUB_REPO_URL" -> githubRepoUrl,
      "GITHUB_BLOB_URL" -> s"$githubRepoUrl/blob/master",
      "HTTP4S_SUFFIX" -> axis.suffix,
      "HTTP4S_VERSION_COMMENT" -> axis.comment,
      "PLAY_LATEST_DEPENDENCY" -> playDepString(PlayAxis.v3_0),
      "PLAY_SUPPORTED_VERSIONS" -> PlayAxis.all.map(a => s"- ${a.version} -- `${playDepString(a)}`").mkString("\n"),
    ),
  ),
).enablePlugins(MdocPlugin)
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
        val (srcFile, targetRelFile) = http4sImplDoc(target / s"${axis.suffix}-jvm-3" / "mdoc", axis)
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

lazy val example = http4sProj(projectMatrix.in(file("example")), "example", _ => List(Platform.Jvm))(
  axis => sjsNowarnGlobalECSettings.andThen(_.andThen(_.settings(
    libraryDependencies ++= Seq(
      http4sDep("circe", axis.version).value,
      http4sDep("blaze-server", axis match {
        case Http4sAxis.v0_23 => s"${axis.suffix}.12"
        case Http4sAxis.v1_0_0_M41 => s"${axis.suffix.dropRight(2)}38"
        case _ => axis.version
      }).value,
    ),
    dependencyOverrides ++= Seq(
      http4sDep("core", axis.version).value
    ),
  ))),
)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      circeDep("core").value,
      circeDep("generic").value,
      "org.slf4j" % "slf4j-api" % "1.7.36",
      "org.slf4j" % "slf4j-simple" % "1.7.36"
    )
  )
  .dependsOn(core, http4s, play)

lazy val githubRepoUrl = "https://github.com/mblink/routing"
