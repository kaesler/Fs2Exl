name := "Fs2Exl"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "eu.timepit" %% "refined" % "0.9.10",
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "co.fs2" %% "fs2-core" % "2.1.0",
  "co.fs2" %% "fs2-io" % "2.1.0"
)

scalacOptions ++= Seq(
    "-feature",
    "-deprecation"
)
