name := "actor4fun"

description := "Actor library in Scala made for educational purpose"

startYear := Option(2020)
homepage  := scmInfo.value map (_.browseUrl)

organization         := "io.univalence"
organizationName     := "Univalence"
organizationHomepage := Some(url("https://univalence.io/"))

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/univalence/actor4fun"),
    "scm:git:https://github.com/univalence/actor4fun.git",
    "scm:git:git@github.com:univalence/actor4fun.git"
  )
)

developers := List(
  Developer(
    id    = "jwinandy",
    name  = "Jonathan Winandy",
    email = "jonathan@univalence.io",
    url   = url("https://github.com/ahoy-jon")
  ),
  Developer(
    id    = "phong",
    name  = "Philippe Hong",
    email = "philippe@univalence.io",
    url   = url("https://github.com/hwki77")
  ),
  Developer(
    id    = "fsarradin",
    name  = "François Sarradin",
    email = "francois@univalence.io",
    url   = url("https://github.com/fsarradin")
  ),
  Developer(
    id    = "bernit77",
    name  = "Bernarith Men",
    email = "bernarith@univalence.io",
    url   = url("https://github.com/bernit77")
  ),
  Developer(
    id    = "HarrisonCheng",
    name  = "Harrison Cheng",
    email = "harrison@univalence.io",
    url   = url("https://github.com/HarrisonCheng")
  )
)

val libVersion = new {
  val logback       = "1.2.3"
  val scala2_12     = "2.12.18"
  val scala2_13     = "2.13.11"
  val slf4j         = "1.7.30"
  val scalacheck    = "1.14.1"
  val scalatest     = "3.2.2"
  val scalatestplus = s"$scalatest.0"
  val scalatestplus_scalacheck = {
    val Some((major, minor)) = CrossVersion.partialVersion(scalacheck)
    s"$major-$minor"
  }
}

scalaVersion       := libVersion.scala2_13
crossScalaVersions := Seq(libVersion.scala2_13, libVersion.scala2_12)

libraryDependencies ++= Seq(
  "ch.qos.logback"        % "logback-classic"      % libVersion.logback,
  "ch.qos.logback"        % "logback-core"         % libVersion.logback,
  "org.slf4j"             % "slf4j-api"            % libVersion.slf4j,
  "io.grpc"               % "grpc-netty"           % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime"      % scalapb.compiler.Version.scalapbVersion % "protobuf"
) ++ Seq(
  "org.scalatest"     %% "scalatest"                                          % libVersion.scalatest,
  "org.scalacheck"    %% "scalacheck"                                         % libVersion.scalacheck,
  "org.scalatestplus" %% s"scalacheck-${libVersion.scalatestplus_scalacheck}" % libVersion.scalatestplus
).map(_ % Test)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

ThisBuild / publishTo := sonatypePublishToBundle.value

onLoadMessage := {
  def header(text: String): String =
    s"${scala.Console.GREEN}$text${scala.Console.RESET}"

  s"""|${header(raw"  __    __   _____  ___   ___     _   ____  _     _     ")}
      |${header(raw" / /\  / /`   | |  / / \ | |_) /_| | | |_  | | | | |\ | ")}
      |${header(raw"/_/--\ \_\_,  |_|  \_\_/ |_| \   |_| |_|   \_\_/ |_| \| ")}
      |${header(s"version: ${Keys.version.value}")}""".stripMargin
}

Global / onChangedBuildSource := ReloadOnSourceChanges
