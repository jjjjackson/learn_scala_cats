ThisBuild / scalaVersion := "2.13.16" // 或 Scala 3

lazy val root = (project in file("."))
  .settings(
    name := "order-service",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.2",
      "org.http4s" %% "http4s-ember-server" % "1.0.0-M40",
      "org.http4s" %% "http4s-dsl" % "1.0.0-M40",
      "org.http4s" %% "http4s-circe" % "1.0.0-M40",
      "io.circe" %% "circe-generic" % "0.14.5",
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
      "ch.qos.logback" % "logback-classic" % "1.4.11"
    )
  )
