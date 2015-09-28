package ru.maizy.dev.heartbeat.actor

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import akka.actor.{ Identify, Actor, ActorLogging, ActorRef, Cancellable, Props }
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.routing.{ Routees, GetRoutees, ActorRefRoutee, AddRoutee, Broadcast, BroadcastPool, RemoveRoutee }

import scala.concurrent.duration._


/** messages */
case object Beat
case object GetStatistics
case object BroadcastBeat
case class ChangeBeatsDelay(newDelay: FiniteDuration)
case class AddSiblings(refs: Seq[ActorRef])
case class RemoveSiblings(refs: Seq[ActorRef])


/** data */
case class Statistics(totalBeatsReceived: BigInt)


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

  def receive: Receive = LoggingReceive(
    handlerBeats orElse
    handleSiblingsMessages
  )

  def handlerBeats: Receive = {
    case Beat =>
      totalBeatsReceived += 1
      printer ! countStats

    case GetStatistics => sender ! countStats

    case BroadcastBeat =>
      siblingNodesPool ! Broadcast(Beat)
      scheduleNextBeat()

    case ChangeBeatsDelay(newDelay: FiniteDuration) =>
      log.info(s"change beats delay to $newDelay")
      cancelNextBeat()
      beatsDelay = newDelay
      scheduleNextBeat()
  }

  def handleSiblingsMessages: Receive = {
    case AddSiblings(refs: Seq[ActorRef]) =>
      log.info(s"add ${refs.size} siblings")
      refs foreach { ref => siblingNodesPool ! AddRoutee(ActorRefRoutee(ref)) }

    case RemoveSiblings(refs: Seq[ActorRef]) =>
      log.info(s"remove ${refs.size} siblings")
      refs foreach { ref => siblingNodesPool ! RemoveRoutee(ActorRefRoutee(ref)) }
  }

  override def postStop(): Unit = {
    cancelNextBeat()
  }

  private def countStats(): Statistics = Statistics(totalBeatsReceived)  // TODO add average beats per 1s, 1m, 5m, 15m

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
