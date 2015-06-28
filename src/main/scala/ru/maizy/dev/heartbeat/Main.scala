package ru.maizy.dev.heartbeat

import scala.concurrent.Future
import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory
import akka.actor.{ PoisonPill, ActorSystem, Props }
import akka.util.Timeout

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
object Main extends App {

  val config = ConfigFactory.load()

  implicit val system = ActorSystem("heartbeat", config)

  val dispatcher = system.dispatcher
  implicit val executionContext = system.dispatcher
  implicit val defaultTimeout = Timeout(500.millis)  //TODO: from config


  //Emulate nodes events

  def emulateEventSeq(events: List[() => Any], _n: Int = 0): Unit = events match {
    case event :: tail =>
      system.log.info(s"emulate event #${_n}")
      event()
      system.scheduler.scheduleOnce(10.second)(emulateEventSeq(tail, _n + 1))
    case _ =>
      system.log.info("no more events to emulate, wait 10 seconds and shutdown")
      system.scheduler.scheduleOnce(10.second)(system.shutdown())
  }

  val reportError: PartialFunction[Throwable, Unit] = {
    case e => system.log.warning(s"oops: $e")
  }

  def reportErrorOrApply[T](future: Future[T])(func: PartialFunction[T, Unit]) {
    future onSuccess func
    future onFailure reportError
  }

  val events = List(
    () => {
      system.log.info("EVENT: add node1")
      system.actorOf(Props[HeartbeatNode], "node1")
    },
    () => {
      system.log.info("EVENT: add node2, connect nodes")
      for (node1 <- system.actorSelection("user/node1").resolveOne) {
          val node2 = system.actorOf(Props[HeartbeatNode], "node2")
          node2 ! AddSibling(node1)
          node1 ! AddSibling(node2)
      }
    },
    () => {
      system.log.info("EVENT: remove node1")
      for (
        node1 <- system.actorSelection("user/node1").resolveOne;
        node2 <- system.actorSelection("user/node2").resolveOne
      ) {
        node2 ! RemoveSibling(node1)
        node1 ! PoisonPill
      }
    },
    () => {
      system.log.info("EVENT: remove node2")
      for (node2 <- system.actorSelection("user/node2").resolveOne) {
        node2 ! PoisonPill
      }
    }
  )

  emulateEventSeq(events)
}
