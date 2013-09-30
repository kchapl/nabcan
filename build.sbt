import play.Project._

name := "nabcan"

version := "1.0"

playScalaSettings

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.10.0",
  "com.github.scala-incubator.io" % "scala-io-file_2.10" % "0.4.2",
  "net.liftweb" %% "lift-json" % "2.5-M4",
  "joda-time" % "joda-time" % "2.2",
  "org.joda" % "joda-convert" % "1.3.1"
)
