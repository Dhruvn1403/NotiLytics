name := """NotiLytics"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  guice,
  "org.playframework" %% "play-ws"      % "3.0.9",
  "org.playframework" %% "play-ahc-ws"  % "3.0.9",
  // Test libs
  "org.junit.jupiter" %  "junit-jupiter" % "5.10.2" % Test,
  "org.mockito"       %  "mockito-core"  % "5.11.0" % Test
)

Test / fork := true
Test / testOptions += Tests.Argument(TestFrameworks.JUnit)

