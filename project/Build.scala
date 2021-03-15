package routing

import java.io.File
import sbt._
import sbt.Keys._
import sbt.internal.ProjectMatrix
import sbtgitpublish.GitPublishKeys._
import sbtprojectmatrix.ProjectMatrixPlugin.autoImport._
import scala.sys.process._

object Build {
  lazy val scalaVersions = Seq("2.12.13", "2.13.5")
  lazy val latestScalaV = scalaVersions.find(_.startsWith("2.13")).get

  def profileTraceOpts(baseDir: File, name: String): Seq[String] = {
    val dir = baseDir / ".traces"
    s"mkdir -p $dir".!!
    Seq("-Yprofile-trace", s"$dir/$name.trace")
  }

  def foldScalaV[A](scalaVersion: String)(_212: => A, _213: => A): A =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 12)) => _212
      case Some((2, 13)) => _213
    }

  def scalaVersionSpecificFolders(srcName: String, srcBaseDir: java.io.File, scalaVersion: String): Seq[java.io.File] =
    Seq(srcBaseDir / srcName / s"scala-${foldScalaV(scalaVersion)("2.12", "2.13")}")

  val commonSettings = Seq(
    organization := "bondlink",
    version := currentVersion,
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full),
    scalacOptions ++= foldScalaV(scalaVersion.value)(Seq(), Seq("-Xlint:strict-unsealed-patmat")),
    // scalacOptions ++= profileTraceOpts(baseDirectory.value, name.value),
    skip in publish := true,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in (Compile, doc) := Seq()
  )

  val publishSettings = Seq(
    skip in publish := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    gitPublishDir := file("/src/maven-repo")
  )

  val noPublishSettings = Seq(
    publish := {},
    publishLocal := {},
    gitRelease := {}
  )

  def projSettings(platform: String, srcDirSuffixes: Seq[String] = Seq(), extra: Project => Project = identity): Project => Project =
    extra.andThen(_.settings(
      unmanagedSourceDirectories in Compile ++=
        (platform +: srcDirSuffixes.flatMap(s => Seq(s, s"$s-$platform")))
          .map(suffix => sourceDirectory.value / "main" / s"scala-$suffix")
    ))

  def proj[A](matrix: ProjectMatrix, nme: String, extraSettings: Project => Project = identity) =
    matrix
      .settings(name := s"routing-$nme")
      .settings(commonSettings)
      .settings(testSettings)
      .customRow(scalaVersions = scalaVersions, axisValues = Seq(VirtualAxis.jvm), projSettings("jvm", extra = extraSettings))
      .customRow(scalaVersions = scalaVersions, axisValues = Seq(VirtualAxis.js), projSettings("js", extra = extraSettings))

  def proj[V <: VirtualAxis](matrix: ProjectMatrix, nme: String, axes: List[(V, String)])(
    srcDirSuffixes: (V, String) => Seq[String],
    extraSettings: (V, String) => Project => Project
  ) =
    axes.foldLeft(matrix
      .settings(name := s"routing-$nme")
      .settings(commonSettings)
      .settings(testSettings)
      .settings(commonSettings)) { case (p, (axis, version)) => p
        .customRow(scalaVersions = scalaVersions, axisValues = Seq(axis, VirtualAxis.jvm),
          projSettings("jvm", srcDirSuffixes(axis, version), extraSettings(axis, version)))
        .customRow(scalaVersions = scalaVersions, axisValues = Seq(axis, VirtualAxis.js),
          projSettings("js", srcDirSuffixes(axis, version), extraSettings(axis, version)))
      }

  def foldHttp4sV[A](version: String)(on_1M: => A, other: => A): A =
    if (isHttp4sV1Milestone(version)) on_1M else other

  def http4sProj(matrix: ProjectMatrix, nme: String)(proc: (Http4sAxis, String) => Project => Project) =
    proj(matrix, nme, http4sVersions)(
      (axis, version) => Seq(version, foldHttp4sV(version)(http4sV1Milestone, axis.suffix)),
      proc)

  val scalacheckVersion = "1.15.3"
  val scalacheckDep = "org.scalacheck" %% "scalacheck" % scalacheckVersion

  val testSettings = Seq(
    libraryDependencies += scalacheckDep % "test",
    testOptions in Test ++= Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2")) ++
      Option(System.getProperty("testIterations")).map(_.toInt)
        .map(n => Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", n.toString)))
        .getOrElse(Seq())
  )

  val catsCore = "org.typelevel" %% "cats-core" % "2.4.2"
  val izumiReflect = "dev.zio" %% "izumi-reflect" % "1.0.0-M16"

  case class Http4sAxis(suffix: String) extends VirtualAxis.WeakAxis {
    val idSuffix = suffix.replace(".", "_")
    val directorySuffix = suffix
  }

  val http4sVersions = List(
    Http4sAxis("0.21") -> "0.21.20",
    Http4sAxis("1.0.0-M10") -> "1.0.0-M10",
    Http4sAxis("1.0.0-M19") -> "1.0.0-M19"
  )
  val http4sV1Milestone = "1.0.0-M"
  def isHttp4sV1Milestone(version: String): Boolean = version.startsWith(http4sV1Milestone)

  def http4sDep(proj: String, version: String) = "org.http4s" %% s"http4s-$proj" % version
  val playCore = "com.typesafe.play" %% "play" % "2.8.2"
}
