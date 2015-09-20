package heartbeat_multijvm_tests

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import scala.collection.mutable
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, Suite }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ CurrentClusterState, MemberUp }
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeSpec, MultiNodeConfig, MultiNodeSpecCallbacks }
import akka.testkit.ImplicitSender


abstract class BaseConfig extends MultiNodeConfig {
  protected val additionalConfigs = ""
  commonConfig(debugConfig(on = false) withFallback ConfigFactory.parseString(
    Seq(
      """
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        actor.provider = "akka.cluster.ClusterActorRefProvider"
      }
      """, additionalConfigs).mkString("\n")
  ))
}


abstract class MultiNodeBaseSpec(config: MultiNodeConfig)
  extends MultiNodeSpec(config)
  with Suite
  with BeforeAndAfterAll
  with MultiNodeSpecCallbacks
  with ImplicitSender
  with Matchers
{

  override def beforeAll() = {
    super.beforeAll()
    multiNodeSpecBeforeAll()
  }

  override def afterAll() = {
    enterBarrier("before-clean-up")
    cleanUp()
    enterBarrier("clean-up")
    multiNodeSpecAfterAll()
    super.afterAll()
  }

  def cluster: Cluster = Cluster(system)
  cluster.subscribe(testActor, classOf[MemberUp])
  expectMsgClass(classOf[CurrentClusterState])
  println(s"myself address: ${node(myself).address}, role: ${myself.name}")

  val currentClusterNodes = mutable.Set[RoleName]()

  def joinToCluster(nodes: Seq[RoleName], seedNode: RoleName): Unit = {
    currentClusterNodes ++= nodes
    // on new nodes await events for all cluster member
    runOn(nodes: _*) {
      cluster join node(seedNode).address
      (receiveN(currentClusterNodes.size).collect { case MemberUp(member) => member.address }.toSet
        should contain theSameElementsAs currentClusterNodes.map(node(_).address).toSet)
    }

    // on existing nodes await events for only new cluster members
    runOn((currentClusterNodes -- nodes.toSet).toList: _*) {
      (receiveN(nodes.size).collect { case MemberUp(member) => member.address }.toSet
        should contain theSameElementsAs nodes.map(node(_).address).toSet)
    }

    enterBarrier("join-"+ nodes.map(_.name).mkString(","))
  }

  def runOnJoinedNodes(a: => Unit): Unit =
    runOn(currentClusterNodes.toList: _*) {
      a
    }

  def cleanUp(): Unit =
    cluster.unsubscribe(testActor)

}
