import sbt._
import Keys._

object CommonDependencies {
  val resolutionRepos = Seq(
    "Concurrent Maven Repo" at "http://conjars.org/repo" // For Scalding, Cascading etc
  )

  object V {
    val scalding  = "0.9.1"
    val hadoop    = "2.2.0"
    val specs2    = "1.13"
    val cascading = "2.5.4"
    val junit = "4.11"
  }

  object Libraries {
    val cascadingCore = "cascading"                %  "cascading-core"       % V.cascading
    val cascadingLocal = "cascading"                %  "cascading-local"       % V.cascading
    val cascadingHadoop = "cascading"                %  "cascading-hadoop2-mr1"       % V.cascading
    val scaldingCore = "com.twitter"                %%  "scalding-core"       % V.scalding exclude( "cascading", "cascading-local" ) exclude( "cascading", "cascading-hadoop" )
    val hadoopCore   = "org.apache.hadoop"          % "hadoop-common"           % V.hadoop       % "provided"
    val hadoopClientCore   = "org.apache.hadoop"          % "hadoop-mapreduce-client-core"           % V.hadoop       % "provided"

    // Scala (test only)
    val specs2       = "org.specs2"                 %% "specs2"               % V.specs2       % "test"
    val junit = "junit" % "junit" % V.junit % "test"
  }
}

object CommonBuildSettings {

  // sbt-assembly settings for building a fat jar
  import sbtassembly.Plugin._
  import AssemblyKeys._

  lazy val defaultScalaSettings = Seq(
    version       := "0.9.1", // -> follow the release numbers of scalding
    scalaVersion  := "2.10.4",
    scalacOptions := Seq("-deprecation", "-encoding", "utf8", "-feature")
  )

  lazy val sbtAssemblySettings = assemblySettings ++ Seq(
    resolvers     ++= CommonDependencies.resolutionRepos,

    // Slightly cleaner jar name
    jarName in assembly <<= (name, version) { (name, version) => name + "-assembly.jar" },
    artifactName in packageBin := { (sv: ScalaVersion, m: ModuleID, a: Artifact) => a.name + "-package.jar" },

    mainClass := Some("run.JobRunner"),

    // Drop these jars
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      val excludes = Set(
        "jsp-api-2.1-6.1.14.jar",
        "jsp-2.1-6.1.14.jar",
        "jasper-compiler-5.5.12.jar",
        "minlog-1.2.jar", // Otherwise causes conflicts with Kyro (which bundles it)
        "janino-2.5.16.jar", // Janino includes a broken signature, and is not needed anyway
        "commons-beanutils-core-1.8.0.jar", // Clash with each other and with commons-collections
        "commons-beanutils-1.7.0.jar",      // "
        "hadoop-core-1.1.2.jar", 
        "hadoop-tools-1.1.2.jar" // "
      )
      cp filter { jar => excludes(jar.data.getName) }
    },

    mergeStrategy in assembly <<= (mergeStrategy in assembly) {
      (old) => {
        case "rootdoc.txt" | "project.clj" => MergeStrategy.discard // Leiningen build files
        case x => old(x)
      }
    }
  )

}


object ScaldingProjectBuild extends Build {

  import CommonDependencies._
  import CommonBuildSettings._

  lazy val buildSettings = defaultScalaSettings ++ sbtAssemblySettings


  // Configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).get(name) + " > " }
  }

  // Define our project, with basic project information and library dependencies
  lazy val project = Project("name", file("."))
    .settings(buildSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        Libraries.cascadingCore,
        Libraries.cascadingLocal,
        Libraries.cascadingHadoop,
        Libraries.scaldingCore,
        Libraries.hadoopCore,
        Libraries.hadoopClientCore,
        Libraries.specs2,
        Libraries.junit
      )
    )
}
