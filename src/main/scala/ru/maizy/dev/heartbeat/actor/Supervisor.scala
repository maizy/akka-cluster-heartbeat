package ru.maizy.dev.heartbeat.actor

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import scala.collection.mutable
import scala.concurrent.duration._
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.event.LoggingReceive

case class StartUp(amountOfStatNodes: Int)
case class Join(otherSupervisors: Seq[ActorRef])
case class AddSupervisor(ref: ActorRef)
case class RemoveSupervisor(ref: ActorRef)


class Supervisor extends Actor with ActorLogging {
  val statActors = mutable.Set[ActorRef]()
  var beatDelay = 2.seconds  // TODO: dynamically change
  var nextIndex = 0

  override def receive: Receive = LoggingReceive {
    case StartUp(amount) =>
      for (index <- 0 until amount) {
        addStatActor()
      }
  }

  private def addStatActor(): ActorRef = {
    val newActorRef = context.actorOf(Props(new Stat(beatDelay)), s"stat-$nextIndex")
    nextIndex += 1
    for (actor <- statActors) {
      actor ! AddSibling(newActorRef)
    }
    statActors += newActorRef
    newActorRef
  }
}
