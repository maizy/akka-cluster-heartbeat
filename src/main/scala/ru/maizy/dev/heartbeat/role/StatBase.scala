package ru.maizy.dev.heartbeat.role

import akka.actor.{ Props, ActorRef, ActorSystem }
import akka.cluster.Cluster

import ru.maizy.dev.heartbeat.actor.{ StartUp, Supervisor }

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
class StatBase extends RoleHandler {

  var supervisor: Option[ActorRef] = None

  override def startUp(system: ActorSystem, cluster: Cluster): Unit = {
    val ref = system.actorOf(Props[Supervisor], name = "supervisor")
    ref ! StartUp(2) //FIXME: from options
    supervisor = Some(ref)
  }

}
