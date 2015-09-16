// file: src/multi-jvm/scala/SupervisortClusterEventsSpec.scala
//
// Required properly multi-jvm setup:
// http://doc.akka.io/docs/akka/snapshot/scala/cluster-usage.html#How_to_Test
//
// based on example from akka docs

import scala.collection.mutable
import scala.concurrent.duration._
import org.scalatest.{ Suite, BeforeAndAfterAll, FlatSpecLike, Matchers }
import com.typesafe.config.ConfigFactory
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ CurrentClusterState, MemberUp }
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeConfig, MultiNodeSpec }
import akka.testkit.ImplicitSender


object SampleDynamicallyChangedClusterSpecConfig extends MultiNodeConfig {

  // register the named roles (nodes) of the test
  val first = role("first")
  val second = role("second")
  val third = role("thrid")
  val fourth = role("fourth")

  def nodeList = Seq(first, second, third, fourth)

  // this configuration will be used for all nodes
  // note that no fixed host names and ports are used
  commonConfig(ConfigFactory.parseString(
    """
    akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
    akka.remote.log-remote-lifecycle-events = off
    akka.cluster.roles = [compute]
    """
  ))

}

// need one concrete test class per node
class SampleDynamicallyChangedClusterSpecMultiJvmNode1 extends SampleDynamicallyChangedClusterSpec
class SampleDynamicallyChangedClusterSpecMultiJvmNode2 extends SampleDynamicallyChangedClusterSpec
class SampleDynamicallyChangedClusterSpecMultiJvmNode3 extends SampleDynamicallyChangedClusterSpec
class SampleDynamicallyChangedClusterSpecMultiJvmNode4 extends SampleDynamicallyChangedClusterSpec


abstract class BaseClusterSpec(config: MultiNodeConfig)
  extends MultiNodeSpec(config: MultiNodeConfig)
  with Suite
  with BeforeAndAfterAll
  with Matchers
  with ImplicitSender
{
  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = {
    enterBarrier("before-clean-up")
    cleanUp()
    enterBarrier("clean-up")
    multiNodeSpecAfterAll()
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


abstract class SampleDynamicallyChangedClusterSpec
  extends BaseClusterSpec(SampleDynamicallyChangedClusterSpecConfig)
  with FlatSpecLike
{

  import SampleDynamicallyChangedClusterSpecConfig._

  override def initialParticipants = roles.size

  val seedNode = first

  "test" should "build cluster join first node to itself" in within(15.seconds) {
    joinToCluster(Seq(first), seedNode)
    testActor ! "test msg"
    expectMsg("test msg")
  }

  it should "join 2 other nodes" in within(15.seconds) {
    joinToCluster(Seq(second, third), seedNode)
    testActor ! "test msg 2"
    expectMsg("test msg 2")
  }

  it should "join last node" in within(15.seconds) {
    joinToCluster(Seq(fourth), seedNode)
    testActor ! "test msg 3"
    expectMsg("test msg 3")
  }

}
