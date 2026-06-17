package routing

import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport.{mimaFailOnNoPrevious, mimaPreviousArtifacts}
import java.io.File
import sbt._
import sbt.Keys._
import sbts3publish.S3PublishPlugin.autoImport.{s3PublishBucket, s3Release}
import scala.sys.process._

object Build {
  lazy val currentVersion = "5.1.0"
  lazy val scalaVersions = Seq("2.13.18", "3.3.8")
  lazy val latestScalaV = scalaVersions.find(_.startsWith("3.")).get
  lazy val kindProjector = compilerPlugin(("org.typelevel" % "kind-projector" % "0.13.4").cross(CrossVersion.full))

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
    Compile / doc / sources := Seq(),
  )

  val publishSettings = Seq(
    licenses += License.Apache2,
    publish / skip := false,
    s3PublishBucket := "bondlink-maven-repo",
    resolvers += "bondlink-maven-repo" at "https://maven.bondlink-cdn.com",
    mimaPreviousArtifacts := Set(organization.value %% (moduleName.value match {
      case s if s.startsWith(s"routing-http4s_$http4sV1Milestone") => s"routing-http4s_${http4sV1Milestone.toLowerCase}44"
      case s => s
    }) % "5.0.0"),
    mimaFailOnNoPrevious := false,
  )

  val noPublishSettings = Seq(
    publish := {},
    publishLocal := {},
    s3Release := {},
    mimaFailOnNoPrevious := false,
  )

  sealed abstract class BuildPlatform(val s: String)
  object BuildPlatform {
    case object Jvm extends BuildPlatform("jvm")
    case object Js extends BuildPlatform("js")
    case object Native extends BuildPlatform("native")
  }

  type ProjSettings = BuildPlatform => Project => Project

  def nativeProj(f: Project => Project): Project => Project =
    f.andThen(_.enablePlugins(scala.scalanative.sbtplugin.ScalaNativePlugin))

  val sjsNowarnGlobalECSettings: ProjSettings =
    _ match {
      case BuildPlatform.Js => _.settings(scalacOptions += "-P:scalajs:nowarnGlobalExecutionContext")
      case BuildPlatform.Jvm | BuildPlatform.Native => identity
    }

  def sjsProj(f: Project => Project): Project => Project =
    f.andThen(_.enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin))

  def platformAxes(platforms: List[BuildPlatform]): List[(BuildPlatform, VirtualAxis)] =
    platforms.map {
      case p @ BuildPlatform.Jvm => (p, VirtualAxis.jvm)
      case p @ BuildPlatform.Js => (p, VirtualAxis.js)
      case p @ BuildPlatform.Native => (p, VirtualAxis.native)
    }

  def baseProj(matrix: ProjectMatrix, nme: String) =
    matrix
      .settings(name := s"routing-$nme")
      .settings(commonSettings)
      .settings(testSettings)

  def projSettings(extra: ProjSettings): ProjSettings =
    platform => {
      val base = extra(platform).andThen(_.settings(
        Compile / unmanagedSourceDirectories += sourceDirectory.value / "main" / s"scala-${platform.s}",
      ))

      platform match {
        case BuildPlatform.Jvm => base
        case BuildPlatform.Js => sjsProj(base)
        case BuildPlatform.Native => nativeProj(base)
      }
    }

  def simpleProj(
    matrix: ProjectMatrix,
    nme: String,
    platforms: List[BuildPlatform],
    extraSettings: ProjSettings = _ => identity,
  ) =
    platformAxes(platforms).foldLeft(baseProj(matrix, nme)) { case (acc, (platform, axis)) =>
      acc.customRow(
        scalaVersions = scalaVersions,
        axisValues = Seq(axis),
        projSettings(extraSettings)(platform),
      )
    }

  def axesProj[V](matrix: ProjectMatrix, nme: String, axes: List[V])(
    platforms: V => List[BuildPlatform],
    extraSettings: V => ProjSettings,
  )(implicit va: V => VirtualAxis) =
    axes
      .flatMap(a => platformAxes(platforms(a)).map(a -> _))
      .foldLeft(baseProj(matrix, nme)) { case (acc, (versionAxis, (platform, platformAxis))) =>
        acc.customRow(
          scalaVersions = scalaVersions,
          axisValues = Seq(va(versionAxis), platformAxis),
          projSettings(extraSettings(versionAxis))(platform),
        )
      }

  var genTimes: Map[(String, String), Long] = Map()

  case class LibAxesProj[V](axes: List[V])(
    suffix: V => String,
    defaultPlatforms: V => List[BuildPlatform],
  ) {
    def apply(matrix: ProjectMatrix, nme: String, platforms: V => List[BuildPlatform] = defaultPlatforms)(
      settings: V => ProjSettings,
    )(implicit va: V => VirtualAxis) =
      axesProj(matrix, nme, axes)(
        platforms,
        axis => platform => proj => settings(axis)(platform)(proj).settings(
          moduleName := s"${name.value}_${suffix(axis)}",
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
                IO.write(outFile, parseSourceFile(src, suffix(axis)))
                outFile
              }
            }
          },
        ),
      )
  }

  val defaultHttp4sPlatforms = (_: Http4sAxis.Value) match {
    case Http4sAxis.v0_23 | Http4sAxis.v1_0_0_M46 => List(BuildPlatform.Jvm, BuildPlatform.Js)
  }
  lazy val http4sProj = LibAxesProj(Http4sAxis.all)(_.suffix, defaultHttp4sPlatforms)

  val defaultPlayPlatforms = (_: PlayAxis.Value) => List(BuildPlatform.Jvm)
  lazy val playProj = LibAxesProj(PlayAxis.all)(_.suffix, defaultPlayPlatforms)

  val scalacheckVersion = "1.19.0"
  val scalacheckDep = "org.scalacheck" %% "scalacheck" % scalacheckVersion

  val testSettings = Seq(
    libraryDependencies += scalacheckDep % Test,
    Test / testOptions ++= Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2")) ++
      Option(System.getProperty("testIterations")).map(_.toInt)
        .map(n => Seq(Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", n.toString)))
        .getOrElse(Seq())
  )

  val catsCore = "org.typelevel" %% "cats-core" % "2.13.0"
  val izumiReflect = "dev.zio" %% "izumi-reflect" % "3.0.9"
  val http4sV1Milestone = "1.0.0-M"

  object Http4sAxis extends Enumeration {
    protected case class HAVal(suffix: String, dep: String => ModuleID, comment: String) extends super.Val { self =>
      lazy val axis: VirtualAxis.WeakAxis =
        new VirtualAxis.WeakAxis {
          val suffix = self.suffix
          val idSuffix = self.suffix.replace(".", "_")
          val directorySuffix = self.suffix
        }
    }

    implicit def valueToHAVal(v: Value): HAVal = v.asInstanceOf[HAVal]
    implicit def valueToVirtualAxis(v: Value): VirtualAxis.WeakAxis = v.axis

    val v0_23 = HAVal("0.23", proj => "org.http4s" %% s"http4s-$proj" % "0.23.34", "latest stable on cats effect 3")
    val v1_0_0_M46 = HAVal(s"${http4sV1Milestone}46", proj => "org.http4s" %% s"http4s-$proj" % "1.0.0-M46", "latest development on cats effect 3")

    lazy val all = values.toList
  }

  object PlayAxis extends Enumeration {
    protected case class PAVal(suffix: String, dep: String => ModuleID) extends super.Val { self =>
      lazy val axis: VirtualAxis.WeakAxis =
        new VirtualAxis.WeakAxis {
          val suffix = self.suffix
          val idSuffix = self.suffix.replace(".", "_")
          val directorySuffix = self.suffix
        }
    }

    implicit def valueToPAVal(v: Value): PAVal = v.asInstanceOf[PAVal]
    implicit def valueToVirtualAxis(v: Value): VirtualAxis.WeakAxis = v.axis

    val v3_0 = PAVal("3.0", proj => "org.playframework" %% proj % "3.0.11")
    val v2_9 = PAVal("2.9", proj => "com.typesafe.play" %% proj % "2.9.11")

    lazy val all = values.toList
  }

  def circeDep(proj: String) = "io.circe" %% s"circe-$proj" % "0.14.15"
}
