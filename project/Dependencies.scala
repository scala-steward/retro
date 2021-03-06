import sbt._
import sbt.Keys._
import scala.language.reflectiveCalls

object Dependencies {

  val V = new {
    val circe = "0.13.0"
    val scalacheck = "1.14.0"
    val scalacheckMagnolia = "0.6.0"
    val mockito = "1.9.5"
    val akka = "2.6.11"
    val akkaHttp = "10.1.11"
    val akkaHttpCirce = "1.35.3"
    val awscala = "0.5.+"
    val cats = "1.6.0"
    val catsEffect = "1.3.0"
    val monixCatnap = "3.0.0-RC2"
    val config = "1.3.0"
    val scalacacheGuava = "0.9.3"
    val scalaLogging = "3.8.0"
    val logback = "1.2.3"
    val leveldb = "1.8"
    val mailin = "3.0.1"
    val jakartaMail = "2.0.0"
    val slick = "3.3.3"
    val postgresql = "42.2.18"
    val mysql = "8.0.23"
    val ldap = "4.0.10"
    val flyway = "5.2.4"
    val bcrypt = "0.4"
    val slf4j = "1.7.25"
    val scalameta = "4.3.22"
    val scalafmtCore = "2.0.0-RC5"
    val plantuml = "8059"
    val pprint = "0.5.9"
    val tapir = "0.14.4"
    val munit = "0.7.3"
  }

  val circeCore = "io.circe" %% "circe-core" % V.circe
  val circeParser = "io.circe" %% "circe-parser" % V.circe
  val circeGeneric = "io.circe" %% "circe-generic" % V.circe
  val circeGenericExtras = "io.circe" %% "circe-generic-extras" % V.circe
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck
  val scalacheckMagnolia = "com.github.chocpanda" %% "scalacheck-magnolia" % V.scalacheckMagnolia
  val mockito = "org.mockito" % "mockito-all" % V.mockito
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % V.akka
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % V.akkaHttp
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % V.akka
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % V.akka
  val awscala = "com.github.seratch" %% "awscala" % V.awscala
  val catsCore = "org.typelevel" %% "cats-core" % V.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
  val alleyCatsCore = "org.typelevel" %% "alleycats-core" % V.cats
  val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % V.akkaHttpCirce
  val typesafeConfig = "com.typesafe" % "config" % V.config
  val scalaCacheGuava = "com.github.cb372" %% "scalacache-guava" % V.scalacacheGuava
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % V.scalaLogging
  val logback = "ch.qos.logback" % "logback-classic" % V.logback
  val levelDb = "org.fusesource.leveldbjni" % "leveldbjni-all" % V.leveldb
  val mailin = "com.sendinblue" % "sib-api-v3-sdk" % V.mailin
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % V.akka
  val jakartaMail = "com.sun.mail" % "jakarta.mail" % V.jakartaMail
  val slick = "com.typesafe.slick" %% "slick" % V.slick
  val slickHikari = "com.typesafe.slick" %% "slick-hikaricp" % V.slick
  val postgresql = "org.postgresql" % "postgresql" % V.postgresql
  val bcrypt = "org.mindrot" % "jbcrypt" % V.bcrypt
  val flywayCore = "org.flywaydb" % "flyway-core" % V.flyway
  val mysql = "mysql" % "mysql-connector-java" % V.mysql
  val ldap = "com.unboundid" % "unboundid-ldapsdk" % V.ldap
  val monixCatnap = "io.monix" %% "monix-catnap" % V.monixCatnap
  val slf4jNop = "org.slf4j" % "slf4j-nop" % V.slf4j
  val scalameta = "org.scalameta" %% "scalameta" % V.scalameta
  val scalafmtCore = "org.scalameta" %% "scalafmt-core" % V.scalafmtCore
  val plantuml = "net.sourceforge.plantuml" % "plantuml" % V.plantuml
  val pprint = "com.lihaoyi" %% "pprint" % V.pprint
  val tapir = "com.softwaremill.sttp.tapir" %% "tapir-core" % V.tapir
  val tapirJsonCirce = "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % V.tapir
  val tapirCore = "com.softwaremill.sttp.tapir" %% "tapir-core" % V.tapir
  val tapirHttp4s = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % V.tapir
  val munit = "org.scalameta" %% "munit" % V.munit
  val munitScalaCheck = "org.scalameta" %% "munit-scalacheck" % V.munit
  val log4j = "org.apache.logging.log4j" % "log4j-api" % "2.13.1"

  val enumeroDependencies = List(
    munit,
    mockito,
  ).map(_ % Test)

  val enumeroCirceDependencies = List(
    circeCore,
  ) ++ List(
    circeParser,
    munit,
  ).map(_ % Test)

  val mailoDependencies = List(
    akkaStream,
    akkaHttp,
    akkaPersistence,
    akkaRemote,
    awscala,
    catsCore,
    alleyCatsCore,
    circeCore,
    circeGeneric,
    circeParser,
    typesafeConfig,
    akkaHttpCirce,
    scalaCacheGuava,
    scalaLogging,
    mailin,
    levelDb,
    jakartaMail,
  ) ++ List(
    munit,
    logback,
    akkaTestkit,
  ).map(_ % Test)

  val toctocCoreDependencies = List(
    bcrypt,
    catsCore,
  ) ++ List(
    munit,
    munitScalaCheck,
    scalacheck,
    scalacheckMagnolia,
    slf4jNop,
    catsEffect,
  ).map(_ % Test)

  val toctocSlickPostgresDependencies = List(
    postgresql,
    slick,
    slickHikari,
    catsEffect,
    monixCatnap,
  ) ++ List(
    munit,
    slf4jNop,
  ).map(_ % Test)

  val toctocSlickMySqlDependencies = List(
    mysql,
    slick,
    slickHikari,
    catsEffect,
    monixCatnap,
  ) ++ List(
    munit,
    slf4jNop,
  ).map(_ % Test)

  val toctocLdapDependencies = List(
    ldap,
    mysql,
    slick,
    slf4jNop,
    catsEffect,
  )

  val toctocCirceDependencies = List(
    circeCore,
    circeGeneric,
  ) ++ List(
    scalacheck,
    scalacheckMagnolia,
    munit,
    munitScalaCheck,
  ).map(_ % Test)

  val metarpheusCoreDependencies = List(
    scalameta,
  ) ++ List(
    munit,
  ).map(_ % Test)

  val metarpheusJsFacadeDependencies = List(
    circeCore,
    circeParser,
    circeGenericExtras,
  )

  val tapiroCoreDependencies = List(
    log4j,
    scalameta,
    scalafmtCore,
    circeCore,
    pprint,
  ) ++ List(
    munit,
  ).map(_ % Test)

  val docsDependencies = List(
    plantuml,
    tapir,
    tapirJsonCirce,
    tapirCore,
    tapirHttp4s,
  )

}
