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

  val opts = new Opts(args)
  opts.afterInit()
  val additionalConfig = s"""
     akka.remote.netty.tcp = {
       port=${opts.port()}
       hostname=${opts.host()}
     }
    """
  val config = ConfigFactory.parseString(additionalConfig).withFallback(ConfigFactory.load())

  implicit val system = ActorSystem("heartbeat", config)

  val dispatcher = system.dispatcher
  implicit val executionContext = system.dispatcher
  implicit val defaultTimeout = Timeout(500.millis)  //TODO: from config


  //Emulate nodes events

  def emulateEventSeq(events: List[() => Any], _n: Int = 0, autoShutdown: Boolean = false): Unit = events match {
    case event :: tail =>
      system.log.info(s"emulate event #${_n}")
      event()
      system.scheduler.scheduleOnce(10.second)(emulateEventSeq(tail, _n + 1))
    case _ =>
      system.log.info("no more events to emulate")
      if (autoShutdown) {
        system.log.info("wait for 10 seconds and shutdown")
        system.scheduler.scheduleOnce(10.second)(system.shutdown _)
      }
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
      system.actorOf(Props(new StatNode(2.second)), "node1")
    },
    () => {
      system.log.info("EVENT: add node2, connect nodes")
      for (node1 <- system.actorSelection("user/node1").resolveOne) {
        val node2 = system.actorOf(Props(new StatNode(2.second)), "node2")
        node2 ! AddSibling(node1)
        node1 ! AddSibling(node2)
      }
    },
    () => {
      system.log.info("EVENT: change beats rate (explicit for every node)")  // TODO: through supervisor node (iss2)
      for (
        node1 <- system.actorSelection("user/node1").resolveOne;
        node2 <- system.actorSelection("user/node2").resolveOne
      ) {
        node1 ! ChangeBeatsDelay(1.second)
        node2 ! ChangeBeatsDelay(1.second)
      }
    },
    () => {
      system.log.info("EVENT: add node3")  // TODO: through supervisor node (iss2)
      for (
        node1 <- system.actorSelection("user/node1").resolveOne;
        node2 <- system.actorSelection("user/node2").resolveOne
      ) {
        val node3 = system.actorOf(Props(new StatNode(1.second)), "node3")
        node2 ! AddSibling(node3)
        node1 ! AddSibling(node3)
        node3 ! AddSibling(node1)
        node3 ! AddSibling(node2)
      }
    },
    () => {
      system.log.info("EVENT: remove node1")
      for (
        node1 <- system.actorSelection("user/node1").resolveOne;
        node2 <- system.actorSelection("user/node2").resolveOne;
        node3 <- system.actorSelection("user/node2").resolveOne
      ) {
        node2 ! RemoveSibling(node1)
        node3 ! RemoveSibling(node1)
        node1 ! PoisonPill
      }
    },
    () => {
      system.log.info("EVENT: remove node3")
      for (
        node3 <- system.actorSelection("user/node3").resolveOne;
        node2 <- system.actorSelection("user/node2").resolveOne
      ) {
        node2 ! RemoveSibling(node3)
        node3 ! PoisonPill
      }
    },
    () => {
      system.log.info("EVENT: remove node2")
      for (node2 <- system.actorSelection("user/node2").resolveOne) {
        node2 ! PoisonPill
      }
    }
  )

  val onlyCreateOneNode = events take 1
  //emulateEventSeq(events, autoShutdown = true)
  //emulateEventSeq(onlyCreateOneNode)
}
