lazy val root = (project in file(".")).
  settings(
    name := "yt-dl",
    version := "0.0.1",
    scalaVersion := "2.11.6",
scalacOptions := Seq("-encoding", "utf8")
  )

libraryDependencies ++= {

  Seq(
    "io.spray" %% "spray-json" % "1.3.2",
		"org.scala-lang.modules" %% "scala-xml" % "1.0.3"
  )
}

