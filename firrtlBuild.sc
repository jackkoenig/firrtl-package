import ammonite.ops._
import ammonite.ops.ImplicitWd._
import mill._
import mill.scalalib._

import $file.common
import $file.BuildInfo
import $file.Protobuf

trait FirrtlModule extends SbtModule with common.CommonOptions with BuildInfo.BuildInfo {

  override def ivyDeps = Agg(
    ivy"com.typesafe.scala-logging::scala-logging:3.7.2",
    ivy"ch.qos.logback:logback-classic:1.2.3",
    ivy"com.github.scopt::scopt:3.7.0",
    ivy"net.jcazevedo::moultingyaml:0.4.0",
    ivy"org.json4s::json4s-native:3.5.3",
    ivy"org.antlr:antlr4-runtime:4.7.1",
    ivy"${Protobuf.ProtobufConfig.ivyDep}"
  )

  def antlrSourceRoot = T.sources{ millSourcePath / 'src / 'main / 'antlr4 }

  def generateAntlrSources(p: Path, sourcePath: Path) = {
    val antlr = new Antlr4Config(sourcePath)
    mkdir! p
    antlr.runAntlr(p)
    p
  }

  def protobufSourceRoot = T.sources{ millSourcePath / 'src / 'main / 'proto }

  def generateProtobufSources(p: Path, sourcePath: Path) = {
    val protobuf = new Protobuf.ProtobufConfig(sourcePath)
    mkdir! p
    protobuf.runProtoc(p)
    p
  }

  override def generatedSources = T {
    val antlrSourcePath: Path = antlrSourceRoot().head.path
    val antlrSources = Seq(PathRef(generateAntlrSources(T.ctx().dest/'antlr, antlrSourcePath)))
    val protobufSourcePath: Path = protobufSourceRoot().head.path
    val protobufSources = Seq(PathRef(generateProtobufSources(T.ctx().dest/'proto, protobufSourcePath)))
    protobufSources ++ antlrSources
  }

  override def buildInfoMembers = T {
    Map[String, String](
      "buildInfoPackage" -> artifactName(),
      "version" -> "1.2-SNAPSHOT",
      "scalaVersion" -> scalaVersion()
    )
  }

  case class Antlr4Config(val sourcePath: Path) {
    val ANTLR4_JAR = (home / 'lib / "antlr-4.7.1-complete.jar").toString
    val antlr4GenVisitor: Boolean = true
    val antlr4GenListener: Boolean = false
    val antlr4PackageName: Option[String] = Some("firrtl.antlr")
    val antlr4Version: String = "4.7"

    val listenerArg: String = if (antlr4GenListener) "-listener" else "-no-listener"
    val visitorArg: String = if (antlr4GenVisitor) "-visitor" else "-no-visitor"
    val packageArg: Seq[String] = antlr4PackageName match {
      case Some(p) => Seq("-package", p)
      case None => Seq.empty
    }
    def runAntlr(outputPath: Path) = {
      val cmd = Seq[String]("java", "-jar", ANTLR4_JAR, "-o", outputPath.toString, "-lib", sourcePath.toString, listenerArg, visitorArg) ++ packageArg :+ (sourcePath / "FIRRTL.g4").toString
      val result = %%(cmd)
    }
  }
  
}
