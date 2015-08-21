import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
abstract class ActorSystemClusterTestCase(system: ActorSystem)
  extends ActorSystemTestCase(system)
{
  def this() = this(ActorSystem(
    "ClusterTestCase",
    ConfigFactory.parseString(ActorSystemClusterTestCase.akkaConfig)))
}

object ActorSystemClusterTestCase {
  def akkaConfig: String = ActorSystemTestCase.akkaConfig +
    """
    akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
    akka.remote.netty.tcp {
      hostname = 127.0.0.1
      port = 0
    }
    """
}
