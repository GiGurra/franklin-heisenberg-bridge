val franklin_heisenberg_bridge = Project(id = "franklin-heisenberg-bridge", base = file("."))
  .settings(
    organization := "se.gigurra",
    version := getVersion,
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation"),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest"    % "2.2.4"   % "test",
      "org.mockito"   % "mockito-core"  % "1.10.19" % "test"
    ),

    resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
  )
  .dependsOn(uri("git://github.com/GiGurra/franklin.git#0.1.10"))
  .dependsOn(uri("git://github.com/GiGurra/heisenberg.git#0.2.6"))

def getVersion: String = {

  val v = scala.util.Properties.envOrNone("FRANKLIN_HEISENBERG_BRIDGE_VERSION").getOrElse {
    println(s"No 'FRANKLIN_HEISENBERG_BRIDGE_VERSION' defined - defaulting to SNAPSHOT")
    "SNAPSHOT"
  }

  println(s"Building Franklin Heisenberg Bridge v. $v")
  v
}

