package routing

import bintray.BintrayKeys._
import java.io.File
import play.twirl.sbt.Import.TwirlKeys
import play.twirl.sbt.SbtTwirl
import sbt._
import sbt.Keys._
import scala.sys.process._

object Build {
  lazy val scalaVersions = Seq("2.12.10", "2.13.1")

  val splainSettings = Seq(
    addCompilerPlugin("io.tryp" % "splain" % "0.4.1" cross CrossVersion.patch),
    scalacOptions ++= Seq(
      "-P:splain:all",
      "-P:splain:foundreq:false",
      "-P:splain:keepmodules:500",
      "-P:splain:rewrite:^((([^\\.]+\\.)*)([^\\.]+))\\.Type$/$1"
    )
  )

  val scala212_opts = Seq(
    "-Xfuture",
    "-Xlint:by-name-right-associative",
    "-Xlint:unsound-match",
    "-Yno-adapted-args",
    "-Ypartial-unification",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit"
  )

  val scala212_213_opts = Seq(
    "-Xlint:adapted-args",
    "-Xlint:constant",
    "-Xlint:delayedinit-select",
    "-Xlint:doc-detached",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:missing-interpolator",
    "-Xlint:nullary-override",
    "-Xlint:nullary-unit",
    "-Xlint:option-implicit",
    "-Xlint:package-object-classes",
    "-Xlint:poly-implicit-overload",
    "-Xlint:private-shadow",
    "-Xlint:stars-align",
    "-Xlint:type-parameter-shadow",
    "-Ywarn-unused:implicits",
    "-Ywarn-unused:imports",
    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates",
    "-Ywarn-extra-implicit",
    "-Ycache-plugin-class-loader:last-modified",
    "-Ycache-macro-class-loader:last-modified"
  )

  def scalaVersionSpecificFolders(srcName: String, srcBaseDir: java.io.File, scalaVersion: String): Seq[java.io.File] =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 12)) => Seq(srcBaseDir / srcName / "scala-2.12")
      case Some((2, 13)) => Seq(srcBaseDir / srcName / "scala-2.13")
      case _ => Seq()
    }

  def profileTraceOpts(baseDir: File, name: String): Seq[String] = {
    val dir = baseDir / ".traces"
    s"mkdir -p $dir".!!
    Seq("-Yprofile-trace", s"$dir/$name.trace")
  }

  val commonSettings = splainSettings ++ Seq(
    organization := "routing",
    crossScalaVersions := scalaVersions,
    scalaVersion := scalaVersions.find(_.startsWith("2.12")).get,
    version := currentVersion,
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-explaintypes",
      "-feature",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Yrangepos",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    ) ++ profileTraceOpts(baseDirectory.value, name.value) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) => scala212_opts ++ scala212_213_opts
      case Some((2, 13)) => scala212_213_opts
      case _ => Seq()
    }),
    scalacOptions in Test ++= profileTraceOpts(baseDirectory.value, s"${name.value}-test"),
    scalacOptions in (Compile, console) := scalacOptions.value.filterNot(x =>
      x.startsWith("-Ywarn-unused") || x.startsWith("-Xlint") || x.startsWith("-P:splain")),
    scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
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

  val catsCore = "org.typelevel" %% "cats-core" % "2.1.0"

  def coreBase = Project("core", file("core"))
    .settings(commonSettings)
    .settings(publishSettings)
    .settings(testSettings)
    .settings(libraryDependencies += catsCore % Optional)
    .settings(Seq(name := "routing-core"))
}
