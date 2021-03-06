package ru.maizy.dev.heartbeat.role

import akka.actor.ActorSystem
import akka.cluster.Cluster


/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
trait RoleHandler {
  def startUp(system: ActorSystem, cluster: Cluster): Unit = {}
}
