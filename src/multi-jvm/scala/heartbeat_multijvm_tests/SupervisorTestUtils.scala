package heartbeat_multijvm_tests

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import akka.actor.{ ActorSelection, ActorRef }
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec


trait SupervisorTestUtils extends MultiNodeSpec {

  def defaultTimeout: FiniteDuration

  def getSupervisorActorForNode(nodeRole: RoleName): Future[ActorRef] =
      getSupervisortSelectForNode(nodeRole).resolveOne()(Timeout.durationToTimeout(defaultTimeout))

  def getSupervisortSelectForNode(nodeRole: RoleName): ActorSelection =
      system.actorSelection(node(nodeRole) / "user" / "supervisor")
}
