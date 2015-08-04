package ru.maizy.dev.heartbeat

import akka.util.Timeout
import ru.maizy.dev.heartbeat.actor.{ RemoveSibling, AddSibling, ChangeBeatsDelay, Stat }
import ru.maizy.dev.heartbeat.utils.EnumerationMap

import scala.concurrent.duration._
import akka.actor.{ Props, ActorSystem, PoisonPill }
/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

object EmulatorProgram extends Enumeration with EnumerationMap {
  type Program = Value

  val ThreeStatNodes = Value("three_stat_nodes")
  val OneStatNode = Value("one_stat_node")
  val OneStatNodeAutoShutdown = Value("one_stat_node_auto_shutdown")
}

class EventEmulator (val autoShutdown: Boolean = false)(implicit system: ActorSystem) {

  implicit val ec = system.dispatcher

  def emulate(events: List[ActorSystem => Unit]) {

    def implementation(xs: Seq[ActorSystem => Any], step: Int = 0): Unit = xs match {
        case event :: tail =>
          system.log.info(s"emulate event #$step")
          event(system)
          system.scheduler.scheduleOnce(10.second)(implementation(tail, step + 1))
        case _ =>
          system.log.info("no more events to emulate")
          if (autoShutdown) {
            system.log.info("wait for 10 seconds and shutdown")
            system.scheduler.scheduleOnce(10.second) {
              system.log.info("doing shutdown from emulator")
              system.shutdown()
            }
          }
      }

    implementation(events, 0)
  }

}

object EventEmulator {

  implicit val defaultTimeout = Timeout(500.millis) //TODO

  val sample1 = List[ActorSystem => Unit](
      (system: ActorSystem) => {
        system.log.info("EVENT: add node1")
        system.actorOf(Props(new Stat(2.second)), "node1")
      },
      (system: ActorSystem) => {
        implicit val ec = system.dispatcher
        system.log.info("EVENT: add node2, connect nodes")
        for (node1 <- system.actorSelection("user/node1").resolveOne) {
          val node2 = system.actorOf(Props(new Stat(2.second)), "node2")
          node2 ! AddSibling(node1)
          node1 ! AddSibling(node2)
        }
      },
      (system: ActorSystem) => {
        implicit val ec = system.dispatcher
        system.log.info("EVENT: change beats rate (explicit for every node)")  // TODO: through supervisor node (iss2)
        for (
          node1 <- system.actorSelection("user/node1").resolveOne;
          node2 <- system.actorSelection("user/node2").resolveOne
        ) {
          node1 ! ChangeBeatsDelay(1.second)
          node2 ! ChangeBeatsDelay(1.second)
        }
      },
      (system: ActorSystem) => {
        implicit val ec = system.dispatcher
        system.log.info("EVENT: add node3")  // TODO: through supervisor node (iss2)
        for (
          node1 <- system.actorSelection("user/node1").resolveOne;
          node2 <- system.actorSelection("user/node2").resolveOne
        ) {
          val node3 = system.actorOf(Props(new Stat(1.second)), "node3")
          node2 ! AddSibling(node3)
          node1 ! AddSibling(node3)
          node3 ! AddSibling(node1)
          node3 ! AddSibling(node2)
        }
      },
      (system: ActorSystem) => {
        implicit val ec = system.dispatcher
        system.log.info("EVENT: remove node1")
        for (
          node1 <- system.actorSelection("user/node1").resolveOne;
          node2 <- system.actorSelection("user/node2").resolveOne;
          node3 <- system.actorSelection("user/node2").resolveOne
        ) {
          node2 ! RemoveSibling(node1)
          node3 ! RemoveSibling(node1)
          node1 ! PoisonPill
        }
      },
      (system: ActorSystem) => {
        implicit val ec = system.dispatcher
        system.log.info("EVENT: remove node3")
        for (
          node3 <- system.actorSelection("user/node3").resolveOne;
          node2 <- system.actorSelection("user/node2").resolveOne
        ) {
          node2 ! RemoveSibling(node3)
          node3 ! PoisonPill
        }
      },
      (system: ActorSystem) => {
        implicit val ec = system.dispatcher
        system.log.info("EVENT: remove node2")
        for (node2 <- system.actorSelection("user/node2").resolveOne) {
          node2 ! PoisonPill
        }
      })


  def emulate(program: EmulatorProgram.Program)(implicit system: ActorSystem): Unit =
      program match {
        case EmulatorProgram.ThreeStatNodes =>
          new EventEmulator(autoShutdown = true) emulate sample1

        case EmulatorProgram.OneStatNode =>
          new EventEmulator emulate (sample1 take 1)

        case EmulatorProgram.OneStatNodeAutoShutdown =>
          new EventEmulator(autoShutdown = true) emulate (sample1 take 1)

        case _ =>
          system.log.error("unsupported emulator program")
          system.shutdown()
      }
}
