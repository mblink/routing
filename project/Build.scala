package routing

import java.io.File
import sbt._
import sbt.Keys._
import sbt.internal.ProjectMatrix
import sbtgitpublish.GitPublishKeys._
import sbtprojectmatrix.ProjectMatrixPlugin.autoImport._
import scala.sys.process._

object Build {
  lazy val scalaVersions = Seq("2.12.15", "2.13.6")
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

  case class ParseState(
    targetVersion: String,
    currVersions: List[String],
    output: List[String]
  )

  val startVersionBlock = "// ++ "
  val endVersionBlock = "// -- "
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
      case (Nil, l) if l.startsWith(startVersionBlock) =>
        acc.copy(
          currVersions = splitVersion(l.stripPrefix(startVersionBlock)),
          output = (acc.output :+ ""))

      // End a version block with a line matching `// -- $version`
      case (vs @ (_ :: _), l) if vs.exists(v => l == endVersionBlock ++ v) =>
        acc.copy(currVersions = Nil, output = (acc.output :+ ""))

      // Fail on attempts at nested version blocks
      case (_ :: _, l) if versionBlockComments.exists(l.startsWith(_)) =>
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
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
    resolvers += "bondlink-maven-repo" at "https://raw.githubusercontent.com/mblink/maven-repo/main",
    addCompilerPlugin("bondlink" %% "nowarn-plugin" % "1.0.1"),
    scalacOptions ++= Seq(
      "-P:nowarn:uu:msg=never used",
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

  var genTimes: Map[(String, String), Long] = Map()

  def http4sProj(matrix: ProjectMatrix, nme: String)(proc: Http4sAxis.Value => ProjSettings) =
    proj(matrix, nme, Http4sAxis.all)(
      axis => Seq(axis.version, if (isHttp4sV1Milestone(axis.version)) http4sV1Milestone else axis.suffix),
      axis => proc(axis).andThen(_.andThen(_.settings(
        moduleName := s"${name.value}_${axis.suffix}",
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
              IO.write(outFile, parseSourceFile(src, axis.version))
              outFile
            }
          }
        }
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

  val catsCore = "org.typelevel" %% "cats-core" % "2.6.1"
  val izumiReflect = "dev.zio" %% "izumi-reflect" % "2.0.1"
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

    val v0_22 = HAVal("0.22", "0.22.4", "latest stable on cats effect 2")
    val v0_23 = HAVal("0.23", "0.23.3", "latest stable on cats effect 3")
    val v1_0_0_M10 = HAVal(s"${http4sV1Milestone}10", s"${http4sV1Milestone}10", "latest development on cats effect 2")
    val v1_0_0_M25 = HAVal(s"${http4sV1Milestone}25", s"${http4sV1Milestone}25", "latest development on cats effect 3")

    lazy val all = values.toList
  }

  def isHttp4sV1Milestone(version: String): Boolean = version.startsWith(http4sV1Milestone)

  def http4sDep(proj: String, version: String) = "org.http4s" %% s"http4s-$proj" % version
  val playCore = "com.typesafe.play" %% "play" % "2.8.8"
}
