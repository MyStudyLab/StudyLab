name := "LifeLab"

version := "0.1.0"

lazy val `lifelab` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(ws, jdbc, evolutions, cache, specs2 % Test)

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.2",
  "mysql" % "mysql-connector-java" % "5.1.41",
  "com.typesafe.play" %% "anorm" % "2.5.1"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator
