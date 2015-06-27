package ru.maizy.dev.heartbeat

import akka.actor.Actor
import akka.event.Logging

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

case object Beat
case object GetHeartbeatStats
case class HeartbeatStats(beatsReceived: BigInt)

class HeartbeatNode extends Actor {

  val log = Logging(context.system, this)

  var received: BigInt = 0

  def receive: Receive = {
    case Beat =>
      log.debug("beat recieved")
      received += 1

    case GetHeartbeatStats =>
      log.debug("send stat")
      sender ! HeartbeatStats(received)
  }
}
