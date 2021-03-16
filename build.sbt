import java.io.File
import routing.currentVersion
import routing.Build._
import scala.sys.process._

Global / onChangedBuildSource := ReloadOnSourceChanges

noPublishSettings

lazy val core = proj(projectMatrix.in(file("core")), "core")
  .settings(publishSettings)
  .settings(
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
        gen("RouteMethods.scala", "route_methods.rb"),
        gen("TupledInstances.scala", "tupled_instances.rb"),
      )
    }
  )

lazy val http4s = http4sProj(projectMatrix.in(file("http4s")), "http4s")((_, version) => _ => _.settings(
  libraryDependencies ++= Seq(
    http4sDep("core", version),
    http4sDep("dsl", version)
  )
))
  .settings(publishSettings)
  .dependsOn(core % "compile->compile;test->test")

lazy val play = proj(projectMatrix.in(file("play")), "play")
  .settings(publishSettings)
  .settings(libraryDependencies += playCore)
  .dependsOn(core % "compile->compile;test->test")

lazy val bench = http4sProj(projectMatrix.in(file("bench")), "bench")((_, _) => _ => identity)
  .settings(noPublishSettings)
  .dependsOn(core, http4s, play)
  .enablePlugins(JmhPlugin)

def http4sImplDoc(dir: File, axis: Http4sAxis, version: String): (File, String) = {
  val f = s"http4s-${axis.suffix}.md"
  (dir / "implementations" / f, s"http4s-${axis.suffix}.md")
}

lazy val docs = http4sProj(projectMatrix.in(file("routing-docs")), "routing-docs")((axis, version) => _.fold(
  _ => identity,
  _ => _.settings(
    mdocExtraArguments ++= "--no-link-hygiene" +: {
      val (_, http4sFile) = http4sImplDoc(mdocIn.value, axis, version)
      Seq("--include", "**.md", "--include", http4sFile) ++
        (mdocIn.value / "implementations").listFiles.map(_.toString.stripPrefix(s"${mdocIn.value}/")).flatMap { f =>
          val fn = f.split('/').last
          if (fn.startsWith("http4s-") && fn != http4sFile) Seq("--exclude", f) else Seq()
        }
    },
    mdocVariables ++= Map(
      "VERSION" -> currentVersion,
      "HTTP4S_VERSION" -> axis.suffix,
      "GITHUB_REPO_URL" -> githubRepoUrl,
      "GITHUB_BLOB_URL" -> s"$githubRepoUrl/blob/master"
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
  val projs = http4sVersions.map { case (axis, version) =>
    (docs.finder(axis, VirtualAxis.jvm)(latestScalaV), axis, version)
  }
  val target = (ThisBuild / baseDirectory).value / "routing-docs" / "target"
  val out = target / "generated-site"
  Def.taskDyn {
    Def.sequential(projs.map { case (p, _, _) => (p / mdoc).toTask("") }).map { _ =>
      IO.delete(out)
      IO.copyDirectory((projs.last._1 / mdocOut).value, out)
      IO.copy(http4sVersions.dropRight(1).map { case (axis, version) =>
        val (srcFile, relFile) = http4sImplDoc(target / s"${axis.suffix}-jvm-2.13" / "mdoc", axis, version)
        println(s"$srcFile -> ${new File(s"$out/implementations/$relFile")}")
        srcFile -> new File(s"$out/implementations/$relFile")
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

lazy val example = http4sProj(projectMatrix.in(file("example")), "example")((_, version) => _ => _.settings(
  libraryDependencies ++= Seq(
    http4sDep("circe", version),
    http4sDep("blaze-server", version)
  )
))
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.13.0",
      "io.circe" %% "circe-generic" % "0.13.0",
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "org.slf4j" % "slf4j-simple" % "1.7.30"
    )
  )
  .dependsOn(core, http4s, play)

lazy val githubRepoUrl = "https://github.com/mblink/routing"
