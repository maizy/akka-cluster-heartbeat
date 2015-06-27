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

  implicit val system = ActorSystem("heartbeat")

  val dispatcher = system.dispatcher
  implicit val executionContext = system.dispatcher

  //FIXME: tmp
  val node1 = system.actorOf(Props[HeartbeatNode], "node1")
  val node2 = system.actorOf(Props[HeartbeatNode], "node2")
  val statPrinter = system.actorOf(Props[HeartbeatStatPrinter], "stat-printer")

  system.scheduler.schedule(0.seconds, 5.second, node1, GetHeartbeatStats)(dispatcher, statPrinter)
  system.scheduler.schedule(0.seconds, 5.second, node2, GetHeartbeatStats)(dispatcher, statPrinter)

  system.scheduler.schedule(1.seconds, 2.second, node1, Beat)(dispatcher, ActorRef.noSender)
  system.scheduler.schedule(2.seconds, 1.second, node2, Beat)(dispatcher, ActorRef.noSender)

}
