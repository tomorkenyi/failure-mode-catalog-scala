name := """failure-mode-catalog-scala"""
organization := "com.ge"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"


libraryDependencies ++= Seq(
  guice,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.6-play26",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)

import play.sbt.routes.RoutesKeys

RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._"
