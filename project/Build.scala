package routing

import java.io.File
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._
import sbt.Keys._
import sbt.internal.ProjectMatrix
import sbtprojectmatrix.ProjectMatrixPlugin.autoImport._
import scala.sys.process._

object Build {
  lazy val scalaVersions = Seq("2.13.16", "3.3.5")
  lazy val latestScalaV = scalaVersions.find(_.startsWith("3.")).get
  lazy val kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full)

  def profileTraceOpts(baseDir: File, name: String): Seq[String] = {
    val dir = baseDir / ".traces"
    s"mkdir -p $dir".!!
    Seq("-Yprofile-trace", s"$dir/$name.trace")
  }

  def foldScalaV[A](scalaVersion: String)(_213: => A, _3: => A): A =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) => _213
      case Some((3, _)) => _3
    }

  case class ParseState(
    targetVersion: String,
    currVersions: List[String],
    output: List[String]
  )

  val startVersionBlock = """^\s*// \+\+ (.*)$""".r
  val endVersionBlock = """^\s*// -- (.*)$""".r
  val versionBlockComments = List(startVersionBlock, endVersionBlock)

  val singleLineVersionComment = """^([^/]+) // (.*)$""".r

  def splitVersion(v: String): List[String] = v.split(",").toList.map(_.trim)

  def parseSourceFile(file: File, targetVersion: String): String =
    scala.io.Source.fromFile(file).getLines().foldLeft(ParseState(
      targetVersion,
      Nil,
      Nil
    ))((acc, line) => (acc.currVersions, line) match {
      // Start a version block with a line matching `// ++ $version`
      case (Nil, startVersionBlock(v)) =>
        acc.copy(currVersions = splitVersion(v), output = (acc.output :+ ""))

      // End a version block with a line matching `// -- $version`
      case (vs @ (_ :: _), endVersionBlock(v)) if vs.exists(_ == v) =>
        acc.copy(currVersions = Nil, output = (acc.output :+ ""))

      // Fail on attempts at nested version blocks
      case (_ :: _, startVersionBlock(_) | endVersionBlock(_)) =>
        sys.error("Cannot nest version blocks")

      // Decide whether to keep a version line within a version block by checking
      // if the target version starts with the current version
      case (vs @ (_ :: _), l) =>
        acc.copy(output = acc.output ++ (if (vs.exists(acc.targetVersion.startsWith(_))) Seq(l) else Seq()))

      // Decide whether to keep a single version specific line by checking
      // if the target version matches the version in the comment
      case (Nil, singleLineVersionComment(l, v)) =>
        acc.copy(output = acc.output ++ (if (splitVersion(v).exists(acc.targetVersion.startsWith(_))) Seq(l) else Seq()))

      // Keep all other lines
      case (Nil, l) =>
        acc.copy(output = (acc.output :+ l))
    }).output.mkString("\n") ++ "\n"

  val commonSettings = Seq(
    organization := "bondlink",
    version := currentVersion,
    scalacOptions ++= foldScalaV(scalaVersion.value)(
      Seq("-Xlint:strict-unsealed-patmat", "-Xsource:3"),
      Seq()
    ),
    libraryDependencies ++= foldScalaV(scalaVersion.value)(
      Seq(kindProjector),
      Seq()
    ),
    // scalacOptions ++= profileTraceOpts(baseDirectory.value, name.value),
    publish / skip := true,
    Compile / packageDoc / publishArtifact := false,
    packageDoc / publishArtifact := false,
    Compile / doc / sources := Seq()
  )

  val publishSettings = Seq(
    licenses += License.Apache2,
    publish / skip := false,
    publishTo := Some("GitHub Packages".at("https://maven.pkg.github.com/mblink/routing")),
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
  )

  val noPublishSettings = Seq(
    publish := {},
    publishLocal := {},
  )

  sealed abstract class Platform(val s: String)
  object Platform {
    case object Jvm extends Platform("jvm")
    case object Js extends Platform("js")
    case object Native extends Platform("native")
  }

  type ProjSettings = Platform => Project => Project

  def nativeProj(f: Project => Project): Project => Project =
    f.andThen(_.enablePlugins(scala.scalanative.sbtplugin.ScalaNativePlugin))

  val sjsNowarnGlobalECSettings: ProjSettings =
    _ match {
      case Platform.Js => _.settings(scalacOptions += "-P:scalajs:nowarnGlobalExecutionContext")
      case Platform.Jvm | Platform.Native => identity
    }

  def sjsProj(f: Project => Project): Project => Project =
    f.andThen(_.enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin))

  def platformAxes(platforms: List[Platform]): List[(Platform, VirtualAxis)] =
    platforms.map {
      case p @ Platform.Jvm => (p, VirtualAxis.jvm)
      case p @ Platform.Js => (p, VirtualAxis.js)
      case p @ Platform.Native => (p, VirtualAxis.native)
    }

  def baseProj(matrix: ProjectMatrix, nme: String) =
    matrix
      .settings(name := s"routing-$nme")
      .settings(commonSettings)
      .settings(testSettings)

  def projSettings(srcDirSuffixes: Seq[String], extra: ProjSettings): ProjSettings =
    platform => {
      val base = extra(platform).andThen(_.settings(
        Compile / unmanagedSourceDirectories ++=
          (platform.s +: srcDirSuffixes.flatMap(s => Seq(s, s"$s-${platform.s}")))
            .map(suffix => sourceDirectory.value / "main" / s"scala-$suffix")
      ))

      platform match {
        case Platform.Jvm => base
        case Platform.Js => sjsProj(base)
        case Platform.Native => nativeProj(base)
      }
    }

  def simpleProj(
    matrix: ProjectMatrix,
    nme: String,
    platforms: List[Platform],
    extraSettings: ProjSettings = _ => identity,
    modScalaVersions: Platform => Seq[String] => Seq[String] = _ => identity,
    includeNative: Boolean = false
  ) =
    platformAxes(platforms).foldLeft(baseProj(matrix, nme)) { case (acc, (platform, axis)) =>
      acc.customRow(
        scalaVersions = modScalaVersions(platform)(scalaVersions),
        axisValues = Seq(axis),
        projSettings(Seq(), extraSettings)(platform),
      )
    }

  def axesProj[V](matrix: ProjectMatrix, nme: String, axes: List[V])(
    platforms: V => List[Platform],
    srcDirSuffixes: V => Seq[String],
    extraSettings: V => ProjSettings,
    modScalaVersions: V => Platform => Seq[String] => Seq[String] = (_: V) => (_: Platform) => identity _,
  )(implicit va: V => VirtualAxis) =
    axes
      .flatMap(a => platformAxes(platforms(a)).map(a -> _))
      .foldLeft(baseProj(matrix, nme)) { case (acc, (versionAxis, (platform, platformAxis))) =>
        acc.customRow(
          scalaVersions = modScalaVersions(versionAxis)(platform)(scalaVersions),
          axisValues = Seq(va(versionAxis), platformAxis),
          projSettings(srcDirSuffixes(versionAxis), extraSettings(versionAxis))(platform),
        )
      }

  var genTimes: Map[(String, String), Long] = Map()

  case class LibAxesProj[V](axes: List[V])(
    version: V => String,
    suffix: V => String,
    suffixSrcDir: V => String,
    defaultSettings: V => ProjSettings,
    defaultPlatforms: V => List[Platform],
    defaultModScalaVersions: V => Platform => Seq[String] => Seq[String],
  ) {
    def apply(matrix: ProjectMatrix, nme: String, platforms: V => List[Platform] = defaultPlatforms)(
      settings: V => ProjSettings,
      modScalaVersions: V => Platform => Seq[String] => Seq[String] = (_: V) => (_: Platform) => identity[Seq[String]],
    )(implicit va: V => VirtualAxis) =
      axesProj(matrix, nme, axes)(
        platforms,
        axis => Seq(version(axis), suffixSrcDir(axis)),
        axis => platform => proj => settings(axis)(platform)(defaultSettings(axis)(platform)(proj)).settings(
          moduleName := s"${name.value}_${suffix(axis).toLowerCase}",
          Compile / sources := {
            val srcs = (Compile / sources).value
            val srcManaged = (Compile / sourceManaged).value / "parsed"
            srcs.map { src =>
              val outFile = srcManaged / src.getName
              val genTimeKey = (src.toString, outFile.toString)
              if (outFile.exists && genTimes.get(genTimeKey).exists(_ > src.lastModified)) {
                outFile
              } else {
                println(s"Writing $src to $outFile")
                genTimes.synchronized { genTimes = genTimes ++ Map(genTimeKey -> System.currentTimeMillis) }
                IO.write(outFile, parseSourceFile(src, version(axis)))
                outFile
              }
            }
          },
        ),
        modScalaVersions = axis => platform =>
          defaultModScalaVersions(axis)(platform).andThen(modScalaVersions(axis)(platform)),
      )
  }

  val defaultHttp4sPlatforms = (_: Http4sAxis.Value) match {
    case Http4sAxis.v0_23 | Http4sAxis.v1_0_0_M44 => List(Platform.Jvm, Platform.Js)
  }
  val defaultHttp4sScalaVersions = (axis: Http4sAxis.Value) => (_: Platform) => axis match {
    case Http4sAxis.v0_23 | Http4sAxis.v1_0_0_M44 => identity[Seq[String]] _
  }

  lazy val http4sProj = LibAxesProj(Http4sAxis.all)(
    _.version,
    _.suffix,
    axis => if (isHttp4sV1Milestone(axis.version)) http4sV1Milestone else axis.suffix,
    _ => _ => identity[Project],
    defaultHttp4sPlatforms,
    defaultHttp4sScalaVersions,
  )

  val defaultPlayPlatforms = (_: PlayAxis.Value) => List(Platform.Jvm)
  val defaultPlayScalaVersions = (axis: PlayAxis.Value) => (_: Platform) => axis match {
    case PlayAxis.v2_9 | PlayAxis.v3_0 => identity[Seq[String]] _
  }

  lazy val playProj = LibAxesProj(PlayAxis.all)(
    _.version,
    _.suffix,
    _.suffix,
    _ => _ => identity[Project],
    defaultPlayPlatforms,
    defaultPlayScalaVersions,
  )

  val scalacheckVersion = "1.18.1"
  val scalacheckDep = Def.setting("org.scalacheck" %%% "scalacheck" % scalacheckVersion)

  val testSettings = Seq(
    libraryDependencies += scalacheckDep.value % "test",
    Test / testOptions ++= Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2")) ++
      Option(System.getProperty("testIterations")).map(_.toInt)
        .map(n => Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", n.toString)))
        .getOrElse(Seq())
  )

  val catsCore = Def.setting("org.typelevel" %%% "cats-core" % "2.13.0")
  val izumiReflect = Def.setting("dev.zio" %%% "izumi-reflect" % "3.0.1")
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

    val v0_23 = HAVal("0.23", "0.23.30", "latest stable on cats effect 3")
    val v1_0_0_M44 = HAVal(s"${http4sV1Milestone}44", s"${http4sV1Milestone}44", "latest development on cats effect 3")

    lazy val all = values.toList
  }

  object PlayAxis extends Enumeration {
    protected case class PAVal(suffix: String, version: String, dep0: String => String => Def.Initialize[ModuleID]) extends super.Val { self =>
      lazy val axis: VirtualAxis.WeakAxis =
        new VirtualAxis.WeakAxis {
          val suffix = self.suffix
          val idSuffix = self.suffix.replace(".", "_")
          val directorySuffix = self.suffix
        }

      val dep: String => Def.Initialize[ModuleID] = dep0(_)(version)
    }

    implicit def valueToPAVal(v: Value): PAVal = v.asInstanceOf[PAVal]
    implicit def valueToVirtualAxis(v: Value): VirtualAxis.WeakAxis = v.axis

    val v3_0 = PAVal("3.0", "3.0.6", proj => version => Def.setting("org.playframework" %%% proj % version))
    val v2_9 = PAVal("2.9", "2.9.6", proj => version => Def.setting("com.typesafe.play" %%% proj % version))

    lazy val all = values.toList
  }

  def isHttp4sV1Milestone(version: String): Boolean = version.startsWith(http4sV1Milestone)

  def circeDep(proj: String) = Def.setting("io.circe" %%% s"circe-$proj" % "0.14.10")
  def http4sDep(proj: String, version: String) = Def.setting("org.http4s" %%% s"http4s-$proj" % version)
}
