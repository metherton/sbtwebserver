import Dependencies._
import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._


val akkaHttp = "com.typesafe.akka" %% "akka-http"   % "10.1.10"
val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.5.23" // or whatever the latest version is
val jsonSerializer = "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10"
val akkaActor = "com.typesafe.akka" %% "akka-actor"   % "2.4.20"
val persistence = "com.typesafe.akka" %% "akka-persistence-query" % "2.6.0"
val cors = "ch.megard" %% "akka-http-cors" % "0.4.2"
val slick1 = "com.typesafe.slick" %% "slick" % "3.3.1"
val slick2 = "org.slf4j" % "slf4j-nop" % "1.7.26"
val slick3 = "com.typesafe.slick" %% "slick-hikaricp" % "3.3.1"
val h2 = "com.h2database" % "h2" % "1.4.199" // See Supported databases, below.
val spring = "org.springframework" % "spring" % "2.5.6"
val logger = "ch.qos.logback" % "logback-classic" % "1.2.3"
val joda = "joda-time" % "joda-time" % "2.10.5"
val mysql = "mysql" % "mysql-connector-java" % "8.0.11"

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.1-SNAPSHOT"
ThisBuild / organization     := "com.martinetherton"
ThisBuild / organizationName := "martinetherton"

lazy val root = (project in file("."))
  .settings(
    name := "server",
    libraryDependencies ++= Seq(scalaTest % Test, akkaHttp, akkaStream, jsonSerializer, akkaActor, cors, persistence, slick1, slick2, slick3, h2, spring, logger, joda, mysql)
  )

// enablePlugins(JavaServerAppPackaging)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

mainClass in Compile := Some("martinetherton.WebServer")
discoveredMainClasses in Compile := Seq()

mappings in Universal ++= {
  // optional example illustrating how to copy additional directory
  directory("scripts") ++
    // copy configuration files to config directory
    contentOf("src/main/resources").toMap.mapValues("config/" + _)
}

// add 'config' directory first in the classpath of the start script,
// an alternative is to set the config file locations via CLI parameters
// when starting the application
scriptClasspath := Seq("../config/") ++ scriptClasspath.value

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))


assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case x => MergeStrategy.first
}

// Uncomment the following for publishing to Sonatype.
// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for more detail.

// ThisBuild / description := "Some descripiton about your project."
// ThisBuild / licenses    := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
// ThisBuild / homepage    := Some(url("https://github.com/example/project"))
// ThisBuild / scmInfo := Some(
//   ScmInfo(
//     url("https://github.com/your-account/your-project"),
//     "scm:git@github.com:your-account/your-project.git"
//   )
// )
// ThisBuild / developers := List(
//   Developer(
//     id    = "Your identifier",
//     name  = "Your Name",
//     email = "your@email",
//     url   = url("http://your.url")
//   )
// )
// ThisBuild / pomIncludeRepository := { _ => false }
// ThisBuild / publishTo := {
//   val nexus = "https://oss.sonatype.org/"
//   if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
//   else Some("releases" at nexus + "service/local/staging/deploy/maven2")
// }
// ThisBuild / publishMavenStyle := true
