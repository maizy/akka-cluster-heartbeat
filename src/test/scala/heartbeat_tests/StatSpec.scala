package heartbeat_tests

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import akka.actor.Props
import akka.testkit.TestActorRef
import org.scalatest.FlatSpecLike
import ru.maizy.dev.heartbeat.actor._

class StatSpec
  extends ActorSystemBaseSpec
  with FlatSpecLike
{

  "Stat actor" should "increase counter on receive beat" in {
    val node = TestActorRef(Props[Stat])
    node.underlyingActor.asInstanceOf[Stat].totalBeatsReceived should be(0)

    node ! Beat
    node.underlyingActor.asInstanceOf[Stat].totalBeatsReceived should be(1)

    for (i <- 0 until 3) {
      node ! Beat
    }
    node.underlyingActor.asInstanceOf[Stat].totalBeatsReceived should be(4)
  }

  it should "return stats on GetHeartbeatStats message" in {
    val node = TestActorRef(Props[Stat])
    for (i <- 0 until 5) {
      node ! Beat
    }

    node ! GetStatistics
    expectMsg(Statistics(5))
  }

}
