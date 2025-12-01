name := "NotiLytics"
organization := "com.example"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.16"
javacOptions ++= Seq("--release", "17")

libraryDependencies ++= Seq(
  guice,

  // Play WS (HTTP client)
  "org.playframework" %% "play-ws"      % "3.0.9",
  "org.playframework" %% "play-ahc-ws"  % "3.0.9",

  // === PEKKO (required by Play 3 – all versions must match) ===
  "org.apache.pekko" %% "pekko-actor"                      % "1.1.2",
  "org.apache.pekko" %% "pekko-actor-typed"                % "1.1.2",
  "org.apache.pekko" %% "pekko-stream"                     % "1.1.2",
  "org.apache.pekko" %% "pekko-slf4j"                      % "1.1.2",
  "org.apache.pekko" %% "pekko-protobuf-v3"                % "1.1.2",
  "org.apache.pekko" %% "pekko-serialization-jackson"      % "1.1.2",

  // === TESTING ===
  "org.junit.jupiter" % "junit-jupiter" % "5.10.2" % Test,

  // Mockito
  "org.mockito"        % "mockito-core"    % "5.11.0" % Test,
  "org.mockito"        % "mockito-inline"  % "5.2.0"  % Test,

  // Pekko Actor TestKit (required for 100% coverage of actors)
  "org.apache.pekko"  %% "pekko-actor-testkit-typed" % "1.1.2" % Test
)

// Force all Pekko versions to stay identical
dependencyOverrides ++= Seq(
  "org.apache.pekko" %% "pekko-actor"                   % "1.1.2",
  "org.apache.pekko" %% "pekko-actor-typed"             % "1.1.2",
  "org.apache.pekko" %% "pekko-stream"                  % "1.1.2",
  "org.apache.pekko" %% "pekko-slf4j"                   % "1.1.2",
  "org.apache.pekko" %% "pekko-protobuf-v3"             % "1.1.2",
  "org.apache.pekko" %% "pekko-serialization-jackson"   % "1.1.2"
)

// JUnit test configuration
Test / fork := true
Test / testOptions += Tests.Argument(TestFrameworks.JUnit)
Test / parallelExecution := false

// Jacoco coverage exclusions
jacocoExcludes := Seq(
  "router.*",
  "Routes*",
  "controllers.routes*",
  "controllers.Reverse*",
  "views.html.*"
)
