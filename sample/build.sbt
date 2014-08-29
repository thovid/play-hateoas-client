name := "play-hateoas-client-sample"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

play.Project.playScalaSettings

lazy val module = RootProject(file("../module"))

lazy val main = project.in(file(".")).dependsOn(module)
