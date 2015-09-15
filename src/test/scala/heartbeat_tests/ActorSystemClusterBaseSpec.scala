package heartbeat_tests

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
abstract class ActorSystemClusterBaseSpec(system: ActorSystem)
  extends ActorSystemBaseSpec(system)
{
  def this() = this(ActorSystem(
    "ClusterTestCase",
    ConfigFactory.parseString(ActorSystemClusterBaseSpec.akkaConfig)))
}

object ActorSystemClusterBaseSpec {
  def akkaConfig: String = ActorSystemBaseSpec.akkaConfig +
    """
    akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
    akka.remote.netty.tcp {
      hostname = 127.0.0.1
      port = 0
    }
    """
}
