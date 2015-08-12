package ru.maizy.dev.heartbeat.actor

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props }
import akka.event.LoggingReceive
import akka.routing.{ ActorRefRoutee, AddRoutee, Broadcast, BroadcastPool, RemoveRoutee }

import scala.concurrent.duration._


/** messages */
case object Beat
case object GetStats
case object BroadcastBeat
case class ChangeBeatsDelay(newDelay: FiniteDuration)

case class AddSibling(node: ActorRef)
case class RemoveSibling(node: ActorRef)
// case object GetSiblings  // TODO

/** data */
case class Stats(totalBeatsReceived: BigInt)


class Stat extends Actor with ActorLogging {

  val printer = context.actorOf(Props[StatsPrinter], "printer")
  val siblingNodesPool = context.actorOf(BroadcastPool(0).props(Props[Stat]), "siblings-pool")

  var beatsDelay: FiniteDuration = 2.seconds
  var lastSchedule: Option[Cancellable] = None
  var totalBeatsReceived: BigInt = 0

  import context.dispatcher

  def this(initialDelay: FiniteDuration) = {
    this()
    beatsDelay = initialDelay
  }

  override def preStart(): Unit = {
    scheduleNextBeat()
  }

  def receive: Receive = LoggingReceive {  // LoggingReceive decorator used for debug only

    case Beat =>
      totalBeatsReceived += 1
      printer ! countStats

    case GetStats => sender ! countStats

    case BroadcastBeat =>
      log.debug("beat")
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

  override def postStop(): Unit = {
    cancelNextBeat()
  }

  private def countStats(): Stats = Stats(totalBeatsReceived)  // TODO

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
