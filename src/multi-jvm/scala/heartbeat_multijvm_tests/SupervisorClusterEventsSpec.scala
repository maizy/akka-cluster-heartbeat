package heartbeat_multijvm_tests

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import scala.concurrent.Future
import scala.concurrent.duration._
import org.scalatest.FlatSpecLike
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorSelection, ActorRef }
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import ru.maizy.dev.heartbeat.actor.{ GetKnownSupervisors, KnownSupervisors }
import ru.maizy.dev.heartbeat.role.Stat


object ClusterEventsMultiNodeConfig extends BaseConfig {
  val statNode0 = role("stat0")
  val statNode1 = role("stat1")
  val statNode2 = role("stat2")
  val noRoleNode = role("no")

  val allNodes = Seq(statNode0, statNode1, statNode2, noRoleNode)
  val statNodes = Seq(statNode0, statNode1, statNode2)
  val seedNode = statNode0

  nodeConfig(statNodes: _*)(ConfigFactory.parseString(
      """
      akka.cluster.roles = ["stat"]
      """
  ))

  nodeConfig(noRoleNode)(ConfigFactory.parseString(
      """
      akka.cluster.roles = ["no"]
      """
  ))
}

// name convention {Spec}MultiJvm{NodeName}
class SupervisorClusterEventsSpecMultiJvmStat0 extends SupervisorClusterEventsSpec
class SupervisorClusterEventsSpecMultiJvmStat1 extends SupervisorClusterEventsSpec
class SupervisorClusterEventsSpecMultiJvmStat2 extends SupervisorClusterEventsSpec
class SupervisorClusterEventsSpecMultiJvmNo extends SupervisorClusterEventsSpec


abstract class SupervisorClusterEventsSpec(config: MultiNodeConfig)
  extends MultiNodeBaseSpec(config)
  with FlatSpecLike
{
  import ClusterEventsMultiNodeConfig._

  def this() = {
    this(ClusterEventsMultiNodeConfig)
  }

  implicit val defaultTimeout = 5.seconds
  implicit val dispatcher = system.dispatcher

  override def initialParticipants: Int = roles.size

  def getSupervisorActorForNode(nodeRole: RoleName): Future[ActorRef] =
      getSupervisortSelectForNode(nodeRole).resolveOne(defaultTimeout)

  def getSupervisortSelectForNode(nodeRole: RoleName): ActorSelection =
      system.actorSelection(node(nodeRole) / "user" / "supervisor")

  def expecMsgWithTimeout[T](msg: T): T = expectMsg[T](defaultTimeout, msg)

  def assertKnownSupervisor(expectedRefs: Set[ActorRef], real: KnownSupervisors): Unit = {
    real.supervisorsRefs should have length expectedRefs.size
    real.supervisorsRefs.foreach { ref => assert(expectedRefs.exists(_.compareTo(ref) == 0)) }  // TODO: optimize?
  }

  muteDeadLetters(classOf[Any])(system)


  "Supervisors" should "wait for all nodes are ready" in within(defaultTimeout) {

    runOn(statNode0) {
      new Stat(2) startUp(system, cluster)
    }
    joinToCluster(Seq(statNode0), seedNode)

    runOnJoinedNodes {
      awaitAssert(cluster.state.members should have size 1)
      awaitAssert(cluster.state.members.map(_.roles) should contain theSameElementsAs Seq(Set("stat")))
    }
    enterBarrier("one node")
  }

  "first stat node" should "ignore nodes without stat role" in within(defaultTimeout) {
    joinToCluster(Seq(noRoleNode), seedNode)
    runOnJoinedNodes {
      awaitAssert(cluster.state.members should have size 2)
      awaitAssert(cluster.state.members.toList.map(_.roles) should contain theSameElementsAs Seq(Set("stat"), Set("no")))
    }

    runOn(statNode0) {
      val firstSupervisor = getSupervisorActorForNode(statNode0).await
      firstSupervisor ! GetKnownSupervisors
      expecMsgWithTimeout(KnownSupervisors(Seq()))
    }

    enterBarrier("no role node")
  }

  it should "listen for cluster event MemberUp and get supervisor from new stat node" in within(defaultTimeout) {
    runOn(statNode1, statNode2) {
      new Stat(2) startUp(system, cluster)
    }
    joinToCluster(Seq(statNode1, statNode2), seedNode)
    enterBarrier("other stat node started")

    runOnJoinedNodes {
      awaitAssert(cluster.state.members should have size 4)
      awaitAssert(
        cluster.state.members.toList.map(_.roles)
          should contain theSameElementsAs
          Seq(Set("stat"), Set("no"), Set("stat"), Set("stat"))
      )

      statNodes.foreach { node =>
        val thatNodeSupervisor = getSupervisortSelectForNode(node)
        val otherNodeSupervisors =
          Future.sequence(statNodes.filter(_ != node).map(getSupervisorActorForNode)).await.toSet
        thatNodeSupervisor ! GetKnownSupervisors
        val supervisors = expectMsgType[KnownSupervisors]
        assertKnownSupervisor(otherNodeSupervisors, supervisors)
      }
    }
    enterBarrier("three nodes ready")
  }

}
