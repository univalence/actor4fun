addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("com.thesamet"  % "sbt-protoc"   % "1.0.0-RC2")
addSbtPlugin("com.dwijnand"  % "sbt-dynver"   % "4.1.1")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8"
