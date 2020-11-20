addSbtPlugin("com.dwijnand"   % "sbt-dynver"   % "4.1.1")
addSbtPlugin("com.jsuereth"   % "sbt-pgp"      % "2.0.1")
addSbtPlugin("com.thesamet"   % "sbt-protoc"   % "1.0.0-RC2")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.5")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8"
