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

  val commonSettings = Seq(
    organization := "bondlink",
    version := currentVersion,
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full),
    scalacOptions ++= Seq(
      // scala 2.12 reports a warning when subclassing `annotation.nowarn` in uu.scala
      // so we silence it here -- https://github.com/scala/bug/issues/10134
      "-Wconf:msg=annotation visible at runtime&src=core/.*/uu.scala:s"
    ) ++ foldScalaV(scalaVersion.value)(Seq(), Seq("-Xlint:strict-unsealed-patmat")),
    // scalacOptions ++= profileTraceOpts(baseDirectory.value, name.value),
    publish / skip := true,
    Compile / packageDoc / publishArtifact := false,
    packageDoc / publishArtifact := false,
    Compile / doc / sources := Seq()
  )

  val publishSettings = Seq(
    publish / skip := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    gitPublishDir := file("/src/maven-repo")
  )

  val noPublishSettings = Seq(
    publish := {},
    publishLocal := {},
    gitRelease := {}
  )

  val platforms = Map[String, Either[VirtualAxis.js.type, VirtualAxis.jvm.type]](
    "js" -> Left(VirtualAxis.js),
    "jvm" -> Right(VirtualAxis.jvm)
  )
  type ProjSettings = Either[VirtualAxis.js.type, VirtualAxis.jvm.type] => Project => Project

  def projSettings(platform: String, srcDirSuffixes: Seq[String], extra: ProjSettings): Project => Project =
    extra(platforms(platform)).andThen(_.settings(
      Compile / unmanagedSourceDirectories ++=
        (platform +: srcDirSuffixes.flatMap(s => Seq(s, s"$s-$platform")))
          .map(suffix => sourceDirectory.value / "main" / s"scala-$suffix")
    ))

  def sjsProj(f: Project => Project): Project => Project =
    f.andThen(_.enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin))

  def proj[A](matrix: ProjectMatrix, nme: String, extraSettings: ProjSettings = _ => identity) =
    matrix
      .settings(name := s"routing-$nme")
      .settings(commonSettings)
      .settings(testSettings)
      .customRow(scalaVersions = scalaVersions, axisValues = Seq(VirtualAxis.jvm), projSettings("jvm", Seq(), extraSettings))
      .customRow(scalaVersions = scalaVersions, axisValues = Seq(VirtualAxis.js), sjsProj(projSettings("js", Seq(), extraSettings)))

  def proj[V](matrix: ProjectMatrix, nme: String, axes: List[V])(
    srcDirSuffixes: V => Seq[String],
    extraSettings: V => ProjSettings
  )(implicit va: V => VirtualAxis) =
    axes.foldLeft(matrix
      .settings(name := s"routing-$nme")
      .settings(commonSettings)
      .settings(testSettings)
      .settings(commonSettings)) { case (p, axis) => p
        .customRow(scalaVersions = scalaVersions, axisValues = Seq(va(axis), VirtualAxis.jvm),
          projSettings("jvm", srcDirSuffixes(axis), extraSettings(axis)))
        .customRow(scalaVersions = scalaVersions, axisValues = Seq(va(axis), VirtualAxis.js),
          sjsProj(projSettings("js", srcDirSuffixes(axis), extraSettings(axis))))
      }

  def http4sProj(matrix: ProjectMatrix, nme: String)(proc: Http4sAxis.Value => ProjSettings) =
    proj(matrix, nme, Http4sAxis.all)(
      axis => Seq(axis.version, if (isHttp4sV1Milestone(axis.version)) http4sV1Milestone else axis.suffix),
      axis => proc(axis).andThen(_.andThen(_.settings(
        moduleName := s"${name.value}_${axis.suffix}"
      ))))

  val scalacheckVersion = "1.15.3"
  val scalacheckDep = "org.scalacheck" %% "scalacheck" % scalacheckVersion

  val testSettings = Seq(
    libraryDependencies += scalacheckDep % "test",
    Test / testOptions ++= Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2")) ++
      Option(System.getProperty("testIterations")).map(_.toInt)
        .map(n => Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", n.toString)))
        .getOrElse(Seq())
  )

  val catsCore = "org.typelevel" %% "cats-core" % "2.6.0"
  val izumiReflect = "dev.zio" %% "izumi-reflect" % "1.1.1"
  val http4sV1Milestone = "1.0.0-M"

  object Http4sAxis extends Enumeration {
    protected case class HAVal(suffix: String, version: String, comment: String) extends super.Val { self =>
      lazy val axis: VirtualAxis.WeakAxis =
        new VirtualAxis.WeakAxis {
          val suffix = self.suffix
          val idSuffix = self.suffix.replace(".", "_")
          val directorySuffix = self.suffix
        }
    }

    implicit def valueToHAVal(v: Value): HAVal = v.asInstanceOf[HAVal]
    implicit def valueToVirtualAxis(v: Value): VirtualAxis.WeakAxis = v.axis

    val v0_21 = HAVal("0.21", "0.21.21", "latest stable")
    val v1_0_0_M10 = HAVal(s"${http4sV1Milestone}10", s"${http4sV1Milestone}10", "latest on cats effect 2")
    val v1_0_0_M21 = HAVal(s"${http4sV1Milestone}21", s"${http4sV1Milestone}21", "latest on cats effect 3")

    lazy val all = values.toList
  }

  def isHttp4sV1Milestone(version: String): Boolean = version.startsWith(http4sV1Milestone)

  def http4sDep(proj: String, version: String) = "org.http4s" %% s"http4s-$proj" % version
  val playCore = "com.typesafe.play" %% "play" % "2.8.8"
}
