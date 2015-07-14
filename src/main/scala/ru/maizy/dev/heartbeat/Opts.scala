package ru.maizy.dev.heartbeat

import org.rogach.scallop.ScallopConf

object Roles extends Enumeration {
  type Role = Value
  val No = Value("no")
  val Stat = Value("stat")
  val Frontend = Value("frontend")
}

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
class Opts(args: Seq[String]) extends ScallopConf(args) {

  // TODO: use separate command for every role ex `stat -a 8`

  lazy val role = opt[String](
    required = true,
    default = Some("stat"),
    descr = "node role",
    argName = "stat|frontend",
    validate = { v => Roles.values.map(_.toString) contains v })

  lazy val statsByNode = opt[Int](
    default = Some(2),
    descr = "amount of stat actors created in one stat node"
  )

  lazy val port = opt[Int](
    required = true,
    validate = { _ <= 65535 }
  )

  lazy val host = opt[String](
    required = true,
    default = Some("127.0.0.1")
  )

  lazy val roleValue = Roles.values.find(_.toString == role()).get

}
