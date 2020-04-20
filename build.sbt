val baseName = "sample-dynamodb-streams-kinesis-adapter"

lazy val root = (project in file("."))
  .settings(
    name := s"$baseName",
    version := "0.1",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.amazonaws" % "dynamodb-streams-kinesis-adapter" % "1.5.1"
    )
  )
