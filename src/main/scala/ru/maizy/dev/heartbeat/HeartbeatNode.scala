package ru.maizy.dev.heartbeat

import scala.concurrent.duration._
import akka.actor.{ ActorRef, Props, Actor, ActorLogging }
import akka.event.{ LoggingReceive, Logging }
import akka.routing._

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

case object Beat
case object GetHeartbeatStats
case object BroadcastBeat
case class HeartbeatStats(beatsReceived: BigInt)

case class AddSibling(node: ActorRef)
case class RemoveSibling(node: ActorRef)
//case object GetSiblings

class HeartbeatNode extends Actor with ActorLogging {

  val printer = context.actorOf(Props[HeartbeatStatsPrinter], "printer")
  val siblingNodesPool = context.actorOf(BroadcastPool(0).props(Props[HeartbeatNode]), "siblings-pool")

  import context.dispatcher
  val schedule = context.system.scheduler.schedule(
    initialDelay = 0.seconds,
    interval = 1.second,
    self,
    BroadcastBeat)

  override def postStop() {
    schedule.cancel()
  }

  var received: BigInt = 0

  def receive: Receive = LoggingReceive {  // LoggingReceive decorator used for debug only

    case Beat =>
      received += 1
      printer ! stats

    case GetHeartbeatStats => sender ! stats

    case BroadcastBeat => siblingNodesPool ! Broadcast(Beat)

    case AddSibling(node: ActorRef) =>
      log.info(s"add sibling ${node.path}")
      siblingNodesPool ! AddRoutee(ActorRefRoutee(node))

    case RemoveSibling(node: ActorRef) =>
      log.info(s"remove sibling ${node.path}")
      siblingNodesPool ! RemoveRoutee(ActorRefRoutee(node))
  }

  private def stats(): HeartbeatStats = HeartbeatStats(received)
}
