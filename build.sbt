name := "Drone-Network"

version := "0.1"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed"           % akkaVersion,
  "com.typesafe.akka" %% "akka-actor"                 % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed"         % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "ch.qos.logback"    %  "logback-classic"             % "1.2.3",
  "com.typesafe.akka" %% "akka-multi-node-testkit"    % akkaVersion % Test,
  "org.scalatest"     %% "scalatest"                  % "3.0.8"     % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed"   % akkaVersion % Test
)