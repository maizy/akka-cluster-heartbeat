package ru.maizy.dev.heartbeat.role

import akka.actor.{ Props, ActorRef, ActorSystem }
import akka.cluster.Cluster

import ru.maizy.dev.heartbeat.actor.{ StartUp, BaseSupervisor }

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
class Stat(val startUpAmountOfStatNodes: Int) extends RoleHandler {

  var supervisor: Option[ActorRef] = None

  override def startUp(system: ActorSystem, cluster: Cluster): Unit = {
    val ref = system.actorOf(Props[BaseSupervisor], name = "supervisor")
    ref ! StartUp(startUpAmountOfStatNodes)
    supervisor = Some(ref)
  }

}
