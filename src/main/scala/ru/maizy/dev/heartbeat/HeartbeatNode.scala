package ru.maizy.dev.heartbeat

import scala.concurrent.duration._
import akka.actor.{ ActorRef, Props, Actor, ActorLogging, Cancellable }
import akka.event.LoggingReceive
import akka.routing.{ BroadcastPool, Broadcast, AddRoutee, RemoveRoutee, ActorRefRoutee }

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

/** messages */
case object Beat
case object GetHeartbeatStats
case object BroadcastBeat
case class ChangeBeatsDelay(newDelay: FiniteDuration)

case class AddSibling(node: ActorRef)
case class RemoveSibling(node: ActorRef)
// case object GetSiblings  //TODO

/** data */
case class HeartbeatStats(totalBeatsReceived: BigInt)


class HeartbeatNode
    extends Actor
    with ActorLogging {

  val printer = context.actorOf(Props[HeartbeatStatsPrinter], "printer")
  val siblingNodesPool = context.actorOf(BroadcastPool(0).props(Props[HeartbeatNode]), "siblings-pool")
  
  var beatsDelay: FiniteDuration = 100.millis
  var lastSchedule: Option[Cancellable] = None
  var totalBeatsReceived: BigInt = 0

  import context.dispatcher

  def this(initialDelay: FiniteDuration) {
    this()
    beatsDelay = initialDelay
  }

  override def preStart() {
    scheduleNextBeat()
  }

  def receive: Receive = LoggingReceive {  // LoggingReceive decorator used for debug only

    case Beat =>
      totalBeatsReceived += 1
      printer ! countStats

    case GetHeartbeatStats => sender ! countStats

    case BroadcastBeat =>
      siblingNodesPool ! Broadcast(Beat)
      scheduleNextBeat()

    case ChangeBeatsDelay(newDelay: FiniteDuration) =>
      log.info(s"change beats delay to $newDelay")
      cancelNextBeat()
      beatsDelay = newDelay
      scheduleNextBeat()
      
    case AddSibling(node: ActorRef) =>
      log.info(s"add sibling ${node.path}")
      siblingNodesPool ! AddRoutee(ActorRefRoutee(node))

    case RemoveSibling(node: ActorRef) =>
      log.info(s"remove sibling ${node.path}")
      siblingNodesPool ! RemoveRoutee(ActorRefRoutee(node))
  }

  override def postStop() {
    cancelNextBeat()
  }

  private def countStats(): HeartbeatStats = HeartbeatStats(totalBeatsReceived)  // TODO

  private def scheduleNextBeat(): Cancellable = {
    val schedule = context.system.scheduler.scheduleOnce(beatsDelay, self, BroadcastBeat)
    lastSchedule = Some(schedule)
    schedule
  }
  
  private def cancelNextBeat(): Unit = {
    lastSchedule.foreach(_.cancel())
    lastSchedule = None
  }
}
