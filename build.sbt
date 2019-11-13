name := "441Project"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "org.cloudsimplus" % "cloudsim-plus" % "4.3.2"

libraryDependencies += "com.typesafe" % "config" % "1.3.2"

// https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-engine
libraryDependencies += "org.junit.jupiter" % "junit-jupiter-engine" % "5.4.2" % Test

// https://mvnrepository.com/artifact/org.easymock/easymock
libraryDependencies += "org.easymock" % "easymock" % "4.0.2" % Test


assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
