package ru.maizy.dev.heartbeat

import akka.actor.Actor
import akka.event.Logging

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
class HeartbeatStatPrinter extends Actor {

  val log = Logging(context.system, this)

  def receive = {
    case s:HeartbeatStats =>
      log.debug("ask for stats printing")

      val stat = s"${sender.path.name} => beats: ${s.beatsReceived}"
      log.info(stat)
      // println(stat)
  }
}
