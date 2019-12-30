import routing.Build._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val core: Project = coreBase

lazy val root: Project = project.in(file("."))
  .settings(commonSettings)
  .settings(bintrayRelease := {})
  .dependsOn(core)
  .aggregate(core)
