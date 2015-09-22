package ru.maizy.dev.heartbeat

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

object Roles extends Enumeration with utils.EnumerationMap {
  type Role = Value
  val No = Value("no")
  val Stat = Value("stat")
  val Frontend = Value("frontend")
}

object Modes extends Enumeration with utils.EnumerationMap {
  type Mode = Value
  val Production = Value("production")
  val Emulator = Value("emulator")
}

case class Options(
    mode: Modes.Mode = Modes.Production,
    port: Int = 0,
    host: String = "127.0.0.1",

    // production mode
    role: Roles.Value = Roles.No,
    statsByNode: Int = 1,

    // emulator mode
    program: Option[EmulatorProgram.Value] = None
)

object OptionParser {

  private val parser = new scopt.OptionParser[Options]("akka-cluster-heartbeat") {
    override def showUsageOnError = true

    private def inEnum(enum: utils.EnumerationMap, value: String) =
      if (enum.valuesMap.contains(value)) {
        success
      } else {
        val allowed = enum.valuesMap.keys
        failure(s"Value '$value' not in allowed values list (${allowed.mkString(", ")})")
      }

    private def enumValues(enum: utils.EnumerationMap) = enum.valuesMap.keys.mkString("|")

    head("akka-cluster-heartbeat", Version.toString)
    help("help")
    version("version")
    opt[String]('h', "host") action { (value, c) => c.copy(host = value) }

    (cmd("node")
      action { (_, c) => c.copy(mode = Modes.Production) }
      text { "production mode (add node to cluster)" }
      children(
        opt[Int]('p', "port")
          text { "port or 0 to random choose" }
          validate { v => if (v < 0 || v > 65535) failure("should be 0 to 65535") else success }
          action { (value, c) => c.copy(port = value) },

        opt[String]('r', "role")
          required()
          valueName enumValues(Roles)
          validate { inEnum(Roles, _) }
          action { (value, c) => c.copy(role = Roles.valuesMap.get(value).get) },

        opt[Int]('s', "stats-by-node")
          validate { v => if (v < 0) failure("should be great than 0") else success }
          action { (value, c) => c.copy(statsByNode = value) }
      )
    )

    (cmd("emulator")
      action { (_, c) => c.copy(mode = Modes.Emulator) }
      text { "emulator mode" }
      children(

        opt[Int]('p', "port")
          action { (value, c) => c.copy(port = value) },

        opt[String]('r', "program")
          required()
          valueName enumValues(EmulatorProgram)
          validate { inEnum(EmulatorProgram, _) }
          action { (value, c) => c.copy(program = EmulatorProgram.valuesMap.get(value)) }
      )
    )
  }

  def parse(args: Seq[String]): Option[Options] =
    parser.parse(args, Options())
}
