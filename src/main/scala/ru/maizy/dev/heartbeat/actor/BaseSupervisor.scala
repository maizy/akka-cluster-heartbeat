package ru.maizy.dev.heartbeat.actor

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.cluster.{ Member, Cluster }
import akka.cluster.ClusterEvent.{ MemberEvent, MemberUp, MemberExited, MemberRemoved, ReachableMember }
import akka.cluster.ClusterEvent.{ UnreachableMember, InitialStateAsEvents }
import akka.actor.{ RootActorPath, Actor, ActorLogging, ActorRef, Props }
import akka.event.LoggingReceive


case class StartUp(amountOfStatNodes: Int)
case class AddSupervisors(refs: Seq[ActorRef])
case class RemoveSupervisors(refs: Seq[ActorRef])
case object GetKnownSupervisors
case class KnownSupervisors(refs: Seq[ActorRef])


class BaseSupervisor extends Actor with ActorLogging {
  val statActors = mutable.Set[ActorRef]()
  val supervisorsActors = mutable.Set[ActorRef]()
  var beatDelay = 2.seconds  // TODO: dynamically change
  var nextIndex = 0
  val cluster = Cluster(context.system)

  import context.dispatcher

  override def receive: Receive = LoggingReceive {
    handlerSupervisorEvents orElse
    handlerClusterEvents
  }

  protected def getStatMemberSupervisor(member: Member, ignoreMyself: Boolean = true): Future[Option[ActorRef]] = {
    val ignore = ignoreMyself && (member.address == cluster.selfAddress)
    if (member.roles.contains("stat") && !ignore) {
      context.actorSelection(RootActorPath(member.address) / "user" / "supervisor")
        .resolveOne(5.second) map { Some(_) }
    } else {
      Future.successful(None)
    }
  }

  protected def handlerClusterEvents: Receive = {
    case MemberUp(member) =>
      val selector = getStatMemberSupervisor(member)

      selector onSuccess {
        case Some(sv) =>
          log.debug(s"Member with supervisor up: $sv")
          self ! AddSupervisors(Seq(sv))
        case _ =>
      }

      selector onFailure {
        case e: Throwable => log.debug(s"Unable to select supervisor on member $member: $e")
      }

    case MemberExited(member) => getStatMemberSupervisor(member) foreach {
      case Some(sv) =>
        log.debug(s"Member with supervisor exited: ${sv.path}")
        self ! RemoveSupervisors(Seq(sv))
      case _ =>
    }
  }

  private def describeActors(actors: Seq[ActorRef]): String =
    actors mkString ", "

  protected def handlerSupervisorEvents: Receive = {
    case StartUp(amount) => startUp(amount)

    case AddSupervisors(actors) =>
      supervisorsActors ++= actors
      log.info(s"Add supervisors ${describeActors(actors)}, set size: ${supervisorsActors.size}");

    case RemoveSupervisors(actors) =>
      supervisorsActors --= actors
      log.info(s"Remove supervisors ${describeActors(actors)}, set size: ${supervisorsActors.size}");

    case GetKnownSupervisors =>
      sender ! KnownSupervisors(supervisorsActors.toList)
  }

  protected def startUp(amount: Int): Unit = {
    for (index <- 0 until amount) {
      addStatActor()
    }
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent],
      classOf[MemberUp], classOf[MemberExited], classOf[MemberRemoved], classOf[ReachableMember],
      classOf[UnreachableMember])
  }

  private def addStatActor(): ActorRef = {
    val newActorRef = context.actorOf(Props(new Stat(beatDelay)), s"stat-$nextIndex")
    nextIndex += 1
    for (actor <- statActors) {
      actor ! AddSibling(newActorRef)
    }
    statActors += newActorRef
    newActorRef
  }
}
