import akka.actor.{ Actor, Props }
import akka.remote.testkit.{ MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks }
import akka.testkit.ImplicitSender
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

//Sample test from akka examples, leave them to check multi-jvm machinery

object SampleConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
}

// name should follow convention {TestName}MultiJvm{NodeName}
class SampleSpecMultiJvmNode1 extends SampleSpec
class SampleSpecMultiJvmNode2 extends SampleSpec

abstract class SampleSpec
  extends MultiNodeSpec(SampleConfig)
  with MultiNodeSpecCallbacks
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender
{

  override def beforeAll() = multiNodeSpecBeforeAll()
  override def afterAll() = multiNodeSpecAfterAll()

  import SampleConfig._
  import SampleSpec._

  def initialParticipants = roles.size

  "A MultiNodeSample" must {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in {
      runOn(node1) {
        enterBarrier("deployed")
        val ponger = system.actorSelection(node(node2) / "user" / "ponger")
        ponger ! "ping"
        expectMsg("pong")
      }

      runOn(node2) {
        system.actorOf(Props[Ponger], "ponger")
        enterBarrier("deployed")
      }

      enterBarrier("finished")
    }
  }
}

object SampleSpec {
  class Ponger extends Actor {
    def receive = {
      case "ping" => sender() ! "pong"
    }
  }
}
