package routing

import bintray.BintrayKeys._
import java.io.File
import sbt._
import sbt.Keys._
import scala.sys.process._

object Build {
  lazy val scalaVersions = Seq("2.12.11", "2.13.1")
  lazy val silencerVersion = "1.6.0"

  val splainSettings = Seq(
    addCompilerPlugin("io.tryp" % "splain" % "0.5.1" cross CrossVersion.patch),
    scalacOptions ++= Seq(
      "-P:splain:all",
      "-P:splain:foundreq:false",
      "-P:splain:keepmodules:500",
      "-P:splain:rewrite:^((([^\\.]+\\.)*)([^\\.]+))\\.Type$/$1"
    )
  )

  def profileTraceOpts(baseDir: File, name: String): Seq[String] = {
    val dir = baseDir / ".traces"
    s"mkdir -p $dir".!!
    Seq("-Yprofile-trace", s"$dir/$name.trace")
  }

  def scalaVersionSpecificFolders(srcName: String, srcBaseDir: java.io.File, scalaVersion: String): Seq[java.io.File] =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 12)) => Seq(srcBaseDir / srcName / "scala-2.12")
      case Some((2, 13)) => Seq(srcBaseDir / srcName / "scala-2.13")
      case _ => Seq()
    }

  val commonSettings = splainSettings ++ Seq(
    organization := "bondlink",
    crossScalaVersions := scalaVersions,
    scalaVersion := scalaVersions.find(_.startsWith("2.13")).get,
    version := currentVersion,
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full),
    addCompilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    libraryDependencies += "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,
    // scalacOptions ++= profileTraceOpts(baseDirectory.value, name.value),
    unmanagedSourceDirectories in Compile ++= scalaVersionSpecificFolders("main", baseDirectory.value, scalaVersion.value),
    unmanagedSourceDirectories in Test ++= scalaVersionSpecificFolders("test", baseDirectory.value, scalaVersion.value),
    skip in publish := true,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in (Compile, doc) := Seq()
  )

  val publishSettings = Seq(
    skip in publish := false,
    bintrayOrganization := Some("bondlink"),
    bintrayRepository := "routing",
    bintrayReleaseOnPublish in ThisBuild := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
  )

  val scalacheckVersion = "1.14.3"
  val scalacheckDep = "org.scalacheck" %% "scalacheck" % scalacheckVersion

  val testSettings = Seq(
    libraryDependencies += scalacheckDep % "test",
    testOptions in Test ++= Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2")) ++
      Option(System.getProperty("testIterations")).map(_.toInt)
        .map(n => Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", n.toString)))
        .getOrElse(Seq())
  )

  val catsCore = "org.typelevel" %% "cats-core" % "2.1.1"
  val izumiReflect = "dev.zio" %% "izumi-reflect" % "0.12.0-M1"

  val http4sVersion = "0.21.3"
  val http4sCore = "org.http4s" %% "http4s-core" % http4sVersion
  val http4sDsl = "org.http4s" %% "http4s-dsl" % http4sVersion

  val playCore = Def.setting {
    "com.typesafe.play" %% "play" % (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) => "2.6.25"
      case Some((2, 13)) => "2.8.1"
    })
  }
}