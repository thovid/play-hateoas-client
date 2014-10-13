name := "play-hateoas-client"

version := "1.0-SNAPSHOT"

organization := "de.thovid"

startYear := Some(2013)

description := "Play framework 2.x module implementing a simple HATEOAS client"

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

scmInfo := Some(ScmInfo(url("https://github.com/thovid/play-hateoas-client"), "https://github.com/thovid/play-hateoas-client.git"))

libraryDependencies ++= Seq(
  "org.mockito" % "mockito-all" % "1.9.0" % "test",
  "no.arktekk" %% "uri-template" % "1.0.2"
)     

play.Project.playScalaSettings
