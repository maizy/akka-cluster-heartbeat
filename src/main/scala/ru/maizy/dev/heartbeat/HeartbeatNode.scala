package ru.maizy.dev.heartbeat

import akka.actor.{ Props, Actor }
import akka.event.{ LoggingReceive, Logging }

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

case object Beat
case object GetHeartbeatStats
case class HeartbeatStats(beatsReceived: BigInt)

class HeartbeatNode extends Actor {

  val log = Logging(context.system, this)
  val printer = context.actorOf(Props[HeartbeatStatsPrinter], "printer")

  var received: BigInt = 0

  def receive: Receive = LoggingReceive {  // LoggingReceive decorator used for debug only
    case Beat =>
      received += 1
      printer ! stats
    case GetHeartbeatStats => sender ! stats
  }

  private def stats(): HeartbeatStats = HeartbeatStats(received)
}
