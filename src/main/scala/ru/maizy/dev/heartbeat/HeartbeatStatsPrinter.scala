package ru.maizy.dev.heartbeat

import akka.actor.Actor
import akka.event.Logging

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
class HeartbeatStatsPrinter extends Actor {

  val log = Logging(context.system, this)

  def receive = {
    case s: HeartbeatStats =>
      log.info(s"${sender().path.name} => beats: ${s.beatsReceived}")
  }
}
