import ReleaseTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

lazy val scala213 = "2.13.1"
lazy val scala212 = "2.12.10"
lazy val scala211 = "2.11.12"

lazy val allScalaVersions = List(scala213, scala212, scala211)
lazy val nativeScalaVersions = List(scala211)

organization in ThisBuild := "io.estatico"

lazy val root = project.in(file("."))
  .aggregate(newtypeJS, newtypeJVM, newtypeNative, catsTestsJVM, catsTestsJS)
  .settings(noPublishSettings)

lazy val newtype = crossProject(JSPlatform, JVMPlatform, NativePlatform).in(file("."))
  .enablePlugins(GitVersioning)
  .settings(defaultSettings)
  .settings(name := "newtype")
  .nativeSettings(
    crossScalaVersions := List("2.11.12"),
    scalaVersion := "2.11.12",
    nativeLinkStubs := true,
    Compile / scalacOptions += "-Yno-predef", // needed to ensure users can use -Yno-predef
    sources in (Compile,doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
  )

lazy val newtypeJVM = newtype.jvm
lazy val newtypeNative = newtype.native
lazy val newtypeJS = newtype.js

lazy val catsTests = crossProject(JSPlatform, JVMPlatform).in(file("cats-tests"))
  .dependsOn(newtype)
  .settings(defaultSettings)
  .settings(noPublishSettings)
  .settings(
    name := "newtype-cats-tests",
    description := "Test suite for newtype + cats interop",
    libraryDependencies += "org.typelevel" %%% "cats-core" % "2.0.0-M4"
  )

lazy val catsTestsJVM = catsTests.jvm
lazy val catsTestsJS = catsTests.js

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val defaultSettings = Seq(
  crossScalaVersions := {
    if (crossProjectPlatform.value == NativePlatform)
      nativeScalaVersions
    else
      allScalaVersions
  },
  homepage := Some(url("https://github.com/estatico/scala-newtype")),
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/estatico/scala-newtype"),
      "scm:git:git@github.com:estatico/scala-newtype.git"
    )
  ),
  developers := List(
    Developer("caryrobbins", "Cary Robbins", "carymrobbins@gmail.com", url("http://caryrobbins.com"))
  ),
  publishMavenStyle := true,
  defaultScalacOptions,
  scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
    case Some((2, n)) if n >= 13 =>
      Seq(
        "-Ymacro-annotations"
      )
  }.toList.flatten,
  bintrayRepository := "jude",
  bintrayOrganization := Some("bryghts"),
  git.useGitDescribe := true,
  git.formattedShaVersion := git.gitHeadCommit.value map { sha => s"v${sha.take(5).toUpperCase}" },
  defaultLibraryDependencies,
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
        )
      case _ =>
        // if scala 2.13.0-M4 or later, macro annotations merged into scala-reflect
        // https://github.com/scala/scala/pull/6606
        Nil
    }
  }
)

lazy val defaultScalacOptions = scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-feature",
  "-deprecation",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros"
) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 11)) =>
    Seq("-Xlint")
  case _ =>
    // on scala 2.12+ some spurious unused warnings get triggered
    Seq("-Xlint:-unused,_")
}) ++ (if (crossProjectPlatform.value == NativePlatform)
    Seq() // Removing the 'predef' on scala native-tests, breaks the test integration with sbt
  else
    Seq("-Yno-predef")) // needed to ensure users can use -Yno-predef

lazy val defaultLibraryDependencies = libraryDependencies ++= Seq(
  scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided,
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
  "org.scalacheck" %%% "scalacheck" % "1.14.3" % Test,
  "org.scalatest" %%% "scalatest" % "3.2.0-M4" % Test,
  "org.scalatestplus" %%% "scalacheck-1-14" % "3.2.0.0-M4" % Test
)


