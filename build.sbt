import Dependencies._
import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import com.typesafe.sbt.packager.docker._


val akkaHttp = "com.typesafe.akka" %% "akka-http"   % "10.2.4"
val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.6.5" // or whatever the latest version is
val jsonSerializer = "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.4"
val akkaActor = "com.typesafe.akka" %% "akka-actor"   % "2.6.5"
val persistence = "com.typesafe.akka" %% "akka-persistence-query" % "2.6.5"
val cors = "ch.megard" %% "akka-http-cors" % "0.4.2"
val slick1 = "com.typesafe.slick" %% "slick" % "3.3.2"
val slick2 = "org.slf4j" % "slf4j-nop" % "1.7.26"
val slick3 = "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2"
val jsonMapper = "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.10"
//val h2 = "com.h2database" % "h2" % "1.4.199" // See Supported databases, below.
//val spring = "org.springframework" % "spring" % "2.6.5"
val logger = "ch.qos.logback" % "logback-classic" % "1.2.3"
val joda = "joda-time" % "joda-time" % "2.10.5"
val mysql = "mysql" % "mysql-connector-java" % "8.0.11"
val httpTest = "com.typesafe.akka" %% "akka-http-testkit" % "10.2.4" % Test
val streamTest = "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.5" % Test
val quartz = "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.5-akka-2.6.x"
val cucumber = "io.cucumber" %% "cucumber-scala" % "6.10.4" % Test

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.3-SNAPSHOT"
ThisBuild / organization     := "com.martinetherton"
ThisBuild / organizationName := "martinetherton"

//lazy val root = (project in file("."))
//  .settings(
//    name := "server",
//    libraryDependencies ++= Seq(scalaTest % Test, akkaHttp, akkaStream, jsonSerializer, akkaActor, cors, persistence, slick1, slick2, slick3, h2, spring, logger, joda, mysql, httpTest, streamTest)
//  )

lazy val root = (project in file("."))
  .settings(
    name := "server",
    libraryDependencies ++= Seq(scalaTest % Test, jsonMapper, akkaHttp, akkaStream, jsonSerializer, akkaActor, persistence, httpTest, streamTest, cors, slick1, slick2, slick3, mysql, logger, joda, quartz, cucumber)
  )


// enablePlugins(JavaServerAppPackaging)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

mainClass in Compile := Some("martinetherton.web.WebServer")
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

dockerCommands+=Cmd("USER", "root")
dockerCommands+= Cmd("RUN" , "keytool -import -noprompt -file /opt/docker/config/fin.cert  -storepass changeit -alias fintech -keystore /usr/local/openjdk-8/jre/lib/security/cacerts")
//dockerCommands+= Cmd("RUN" , "keytool -import -noprompt -file /opt/docker/config/www.martinetherton.com.crt -storepass changeit -alias metherton -keystore /usr/local/openjdk-8/jre/lib/security/cacerts")
//dockerCommands+= Cmd("RUN" , "keytool -import -noprompt -file /opt/docker/config/newsecureserver.crt -storepass changeit -alias methertonnew -keystore /usr/local/openjdk-8/jre/lib/security/cacerts")
dockerCommands+= Cmd("RUN" , "keytool -import -noprompt -file /opt/docker/config/localhost.crt -storepass changeit -alias server -keystore /usr/local/openjdk-8/jre/lib/security/cacerts")
// dockerCommands+= Cmd("RUN" , "cp /opt/docker/config/newsecureserver.crt /usr/local/openjdk-8/jre/lib/security/cacerts")


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
