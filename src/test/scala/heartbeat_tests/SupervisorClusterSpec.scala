package heartbeat_tests

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import scala.concurrent.duration._
import akka.actor.Props
import akka.testkit.TestActorRef
import org.scalatest.FlatSpecLike
import ru.maizy.dev.heartbeat.actor._


class SupervisorClusterSpec
  extends ActorSystemClusterTestCase
  with FlatSpecLike
{

  trait TestSupervisors {
    val sv1 = TestActorRef(Props[BaseSupervisor])
    val sv2 = TestActorRef(Props[BaseSupervisor])
    val sv3 = TestActorRef(Props[BaseSupervisor])

    val all = Seq(sv1, sv2, sv3)

    all.foreach { _ ! StartUp(2) }
  }

  "Supervisor" should "init stat actors on startup" in {
    val sv = TestActorRef(Props[BaseSupervisor])
    sv.children.toList should have length 0

    sv ! StartUp(3)
    sv.children.toList should have length 3
  }

  it should "add supervisors" in {
    new TestSupervisors {
      sv1 ! GetKnownSupervisors
      expectMsg(KnownSupervisors(Nil))

      sv1 ! AddSupervisors(Seq(sv2, sv3))
      sv1 ! GetKnownSupervisors
      val res = receiveOne(100.millis)
      res shouldBe a[KnownSupervisors]
      res.asInstanceOf[KnownSupervisors].refs should contain theSameElementsAs Seq(sv2, sv3)
    }
  }

  it should "remove supervisors" in {
    new TestSupervisors {
      sv1 ! AddSupervisors(Seq(sv2, sv3))
      sv1 ! RemoveSupervisors(Seq(sv3))

      sv1 ! GetKnownSupervisors
      expectMsg(KnownSupervisors(Seq(sv2)))

      sv1 ! RemoveSupervisors(Seq(sv2))

      sv1 ! GetKnownSupervisors
      expectMsg(KnownSupervisors(Nil))

    }

  }

  // TODO
  // it should "return start actors on GetStatActors" in
  // it should "listen MemberUp cluster event and add member supervisor on it" in
  // it should "listen MemberExit cluster event and remove member supervisor on it" in
}
