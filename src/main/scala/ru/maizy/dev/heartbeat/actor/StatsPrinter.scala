package ru.maizy.dev.heartbeat.actor

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import akka.actor.Actor
import akka.event.Logging


class StatsPrinter extends Actor {

  val log = Logging(context.system, this)

  def receive = {
    case s: Stats =>
      log.info(s"${sender().path.name} => beats: ${s.totalBeatsReceived}")
  }
}
