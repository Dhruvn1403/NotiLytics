name := """NotiLytics"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.16"
javacOptions ++= Seq("--release", "17")

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

// ---- TEST DEPENDENCIES ----
libraryDependencies ++= Seq(
  "junit" % "junit" % "4.13.2" % Test,
  "org.mockito" % "mockito-core" % "5.12.0" % Test,
  "org.mockito" % "mockito-inline" % "5.2.0" % Test // allows mocking some finals
)

// ---- JACOCO & TEST SETTINGS ----
Test / parallelExecution := false

// Exclude Play auto-generated stuff from coverage (allowed by spec)
jacocoExcludes := Seq(
  "router.*",
  "Routes*",
  "controllers.routes*",
  "controllers.Reverse*",
  "views.html.*"
)
