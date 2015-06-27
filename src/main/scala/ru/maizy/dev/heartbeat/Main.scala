package ru.maizy.dev.heartbeat

import scala.concurrent.duration._

import akka.actor.{ ActorRef, ActorSystem, Props }
import com.typesafe.config.ConfigFactory

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
object Main extends App {

  val config = ConfigFactory.load()

  implicit val system = ActorSystem("heartbeat", config)

  val dispatcher = system.dispatcher
  implicit val executionContext = system.dispatcher

  //FIXME: tmp
  val node1 = system.actorOf(Props[HeartbeatNode], "node1")
  val node2 = system.actorOf(Props[HeartbeatNode], "node2")

  system.scheduler.schedule(initialDelay = 0.seconds, interval = 1.second, node2, Beat)(dispatcher, node1)
  system.scheduler.schedule(initialDelay = 1.seconds, interval = 2.second, node1, Beat)(dispatcher, node2)
}
