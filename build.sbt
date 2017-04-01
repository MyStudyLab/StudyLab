name := "Dashboard"

version := "1.0"

lazy val `dashboard` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"


// Enable CORS for the hackathon
// TODO: remove this once the site is live
libraryDependencies ++= Seq(filters, jdbc, cache, ws, specs2 % Test)

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.9"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator
