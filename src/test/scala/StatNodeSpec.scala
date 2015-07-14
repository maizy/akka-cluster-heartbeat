/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit, TestActorRef }
import org.scalatest.{ FlatSpecLike, Matchers }

import ru.maizy.dev.heartbeat.{ Stats, GetStats, StatNode, Beat }

class StatNodeSpec
    extends TestKit(ActorSystem("testStatNode"))
    with FlatSpecLike
    with Matchers
    with ImplicitSender
    with KillActorSystemAfterAllTests
{

  "HeartbeatNode" should "increase counter on receive beat" in {
    val node = TestActorRef(Props[StatNode])
    node.underlyingActor.asInstanceOf[StatNode].totalBeatsReceived should be(0)

    node ! Beat
    node.underlyingActor.asInstanceOf[StatNode].totalBeatsReceived should be(1)

    for (i <- 0 until 3) {
      node ! Beat
    }
    node.underlyingActor.asInstanceOf[StatNode].totalBeatsReceived should be(4)
  }

  it should "return stats on GetHeartbeatStats message" in {
    val node = TestActorRef(Props[StatNode])
    for (i <- 0 until 5) {
      node ! Beat
    }

    node ! GetStats
    expectMsg(Stats(5))
  }

}
