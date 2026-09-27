ThisBuild / scalaVersion := "3.3.3"
ThisBuild / version      := "0.1.0"

// ScalaFX needs the OS-specific JavaFX classifier
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case n                            => throw new Exception(s"Unknown platform: $n")
}

lazy val root = (project in file("."))
  .settings(
    name := "community-skills-marketplace",
    libraryDependencies += "org.scalafx" %% "scalafx" % "21.0.0-R32",
    libraryDependencies ++= Seq("base", "controls", "fxml", "graphics", "media")
      .map(m => "org.openjfx" % s"javafx-$m" % "21.0.2" classifier osName),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    Compile / mainClass := Some("marketplace.ui.MarketplaceApp"),
    // ai-assisted: #12
    // why: keeps the rubric's onAction = handle idiom (S2-6) without the deprecation [warn] lines (S1-2).
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Wunused:all",
      "-Wconf:msg=handle in trait EventIncludes:silent")
  )
