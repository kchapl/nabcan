import sbt._

object ApplicationBuild extends Build {

  val appName = "nabcan"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "net.databinder.dispatch" %% "dispatch-core" % "0.10.0",
    "com.github.scala-incubator.io" % "scala-io-file_2.10" % "0.4.2",
    "net.liftweb" %% "lift-json" % "2.5-M4",
    "joda-time" % "joda-time" % "2.2",
    "org.joda" % "joda-convert" % "1.3.1"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
  )

}
