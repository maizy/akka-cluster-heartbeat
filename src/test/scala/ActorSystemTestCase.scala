import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.ConfigFactory
import org.scalatest.Suite

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
abstract class ActorSystemTestCase(system: ActorSystem)
    extends TestKit(system)
    with Suite
    with ImplicitSender
    with KillActorSystemAfterAllTests {

  def this() = {
    this(ActorSystem("TestCase", ConfigFactory.parseString(
      """
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        loglevel = WARNING
      }
      """)))
  }
}
