val franklin_heisenberg_bridge = Project(id = "franklin-heisenberg-bridge", base = file("."))
  .settings(
    organization := "com.github.gigurra",
    version := "0.1.20-SNAPSHOT",
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation"),

    libraryDependencies ++= Seq(
      "com.github.gigurra"  %% "franklin"     % "0.1.11",
      "com.github.gigurra"  %% "heisenberg"   % "0.2.8",
      "org.scalatest"       %% "scalatest"    % "2.2.4"   % "test",
      "org.mockito"          % "mockito-core" % "1.10.19" % "test"
    ),

    resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
  )

