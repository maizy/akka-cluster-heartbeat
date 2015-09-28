package heartbeat_multijvm_tests

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import akka.actor.ActorRef

import scala.concurrent.duration._
import org.scalatest.FlatSpecLike
import com.typesafe.config.ConfigFactory
import akka.remote.testkit.MultiNodeConfig
import ru.maizy.dev.heartbeat.actor.{ Statistics, GetStatistics, StatActors, GetStatActors }
import ru.maizy.dev.heartbeat.role.Stat


object StatBeatsSpecMultiNodeConfig extends BaseConfig {
  val statNode0 = role("stat0")
  val statNode1 = role("stat1")

  val allNodes = Seq(statNode0, statNode1)
  val seedNode = statNode0

  nodeConfig(allNodes: _*)(ConfigFactory.parseString(
      """
      akka.cluster.roles = ["stat"]
      """
  ))
}

// name convention {Spec}MultiJvm{NodeName}
class StatActorManagedBySupervisorSpecMultiJvmStat0 extends StatActorManagedBySupervisorSpec
class StatActorManagedBySupervisorSpecMultiJvmStat1 extends StatActorManagedBySupervisorSpec


//TODO: descrese beats delay for speedup tests
abstract class StatActorManagedBySupervisorSpec(config: MultiNodeConfig)
  extends MultiNodeBaseSpec(config)
  with FlatSpecLike
  with SupervisorTestUtils
{
  import StatBeatsSpecMultiNodeConfig._

  def this() = {
    this(StatBeatsSpecMultiNodeConfig)
  }

  implicit val defaultTimeout = 5.seconds
  implicit val dispatcher = system.dispatcher
  val expectedBeatDelay = 2.seconds  // TODO: set for tested actors, when supported

  override def initialParticipants: Int = roles.size

  muteDeadLetters(classOf[Any])(system)

  var maybeFirstStatActor: Option[ActorRef] = None

  "StatActor managed by supervisor" should "launch on supervisor startup" in within(defaultTimeout) {

    runOn(statNode0) {
      new Stat(1) startUp(system, cluster)
    }
    joinToCluster(Seq(statNode0), seedNode)
    awaitClusterReady(Seq("stat"))
    
    runOnJoinedNodes {
      val supervisor = getSupervisorActorForNode(statNode0).await
      supervisor ! GetStatActors
      val statActors = expectMsgType[StatActors]
      statActors.me shouldBe supervisor
      statActors.statActorsRefs should have size 1
      
      maybeFirstStatActor = Some(statActors.statActorsRefs.head)
    }
  }
  
  it should "do nothing if there are no siblings" in within(defaultTimeout) {
    runOnJoinedNodes {
      val statActor = maybeFirstStatActor.get

      statActor ! GetStatistics
      expectMsg(Statistics(totalBeatsReceived = 0))
    }
  }
  
  it should "recive beats from siblings of other nodes" in within(defaultTimeout + expectedBeatDelay * 3) {
    runOn(statNode1) {
      new Stat(1) startUp(system, cluster)
    }
    joinToCluster(Seq(statNode1), seedNode)
    awaitClusterReady(Seq("stat", "stat"))

    runOnJoinedNodes {
      val secondSupervisor = getSupervisorActorForNode(statNode1).await
      secondSupervisor ! GetStatActors
      val statActors = expectMsgType[StatActors]
      statActors.me shouldBe secondSupervisor
      statActors.statActorsRefs should have size 1

      val secondStatActor = statActors.statActorsRefs.head
      awaitAssert(
        {
          secondStatActor ! GetStatistics
          val statistics = expectMsgType[Statistics]
          statistics.totalBeatsReceived should be > BigInt(2)
        },
        expectedBeatDelay * 3
      )
    }
  }

}
