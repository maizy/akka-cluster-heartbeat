import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{ MultiJvm, jvmOptions }

enablePlugins(JavaServerAppPackaging)

val akkaVersion = "2.3.12"
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
lazy val testScalastyleInCompile = taskKey[Unit]("testScalastyleInCompile")

val project = Project(
  id = "akka-cluster-heartbeat",
  base = file("."),
  settings = Defaults.coreDefaultSettings ++ SbtMultiJvm.multiJvmSettings ++ Seq(
    name := "akka-cluster-heartbeat",
    organization := "ru.maizy.dev",
    version := "0.1",
    scalaVersion := "2.11.7",
    libraryDependencies ++= {
      Seq(
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "com.github.scopt" %% "scopt" % "3.3.0",
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-remote" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
        "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % "test",
        "org.scalatest" %% "scalatest" % "2.2.0" % "test"
      )
    },
    scalacOptions ++= Seq(
      "-target:jvm-1.7",
      "-encoding", "UTF-8",
      "-deprecation",
      "-unchecked",
      "-explaintypes",
      "-Xfatal-warnings",
      "-Xlint"
    ),
    fork in run := true,
    resolvers ++=Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      Resolver.sonatypeRepo("public")
    ),

    // Scalastyle setup
    // TODO: check code under multi-jvm dir
    testScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Test).toTask("").value,
    testScalastyleInCompile := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value,
    (test in Test) <<= (test in Test) dependsOn (testScalastyle, testScalastyleInCompile),
    scalastyleFailOnError := true,

    // MultiJVM setup
    // make sure that MultiJvm test are compiled by the default test compilation
    compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),

    // disable parallel tests
    parallelExecution in Test := false,

    // make sure that MultiJvm tests are executed by the default test target,
    // and combine the results from ordinary test and multi-jvm tests
    executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
      case (testResults, multiNodeResults)  =>
        val overall =
          if (testResults.overall.id < multiNodeResults.overall.id)
            multiNodeResults.overall
          else
            testResults.overall
        Tests.Output(overall,
          testResults.events ++ multiNodeResults.events,
          testResults.summaries ++ multiNodeResults.summaries)
    }
  )
) configs MultiJvm
