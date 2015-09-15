package heartbeat_tests

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ Matchers, Suite }

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
abstract class ActorSystemBaseSpec(system: ActorSystem)
  extends TestKit(system)
  with Suite
  with ImplicitSender
  with Matchers
  with KillActorSystemAfterAllTests
{

  def this() = this(ActorSystem(
    "TestCase",
    ConfigFactory.parseString(ActorSystemBaseSpec.akkaConfig)))
}

object ActorSystemBaseSpec {
  def akkaConfig: String =
    """
    akka {
      loggers = ["akka.testkit.TestEventListener"]
      loglevel = WARNING
      actor.debug = {
        lifecycle = on
        receive = on
      }
    }
    """
}
