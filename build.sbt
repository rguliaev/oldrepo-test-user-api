
name := "test-user-api"

version := "0.1"

scalaVersion := "2.12.3"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies ++=
  Seq(
    "com.github.t3hnar" %% "scala-bcrypt" % "3.1",
    "com.typesafe.akka" %% "akka-http" % "10.1.3",
    "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.3" % Test
  ) ++
  Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser").map(_ % "0.9.3")