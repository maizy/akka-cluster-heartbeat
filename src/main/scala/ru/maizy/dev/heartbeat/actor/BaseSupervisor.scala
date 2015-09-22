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

/* messages */
case class StartUp(amountOfStatNodes: Int)
case class AddSupervisors(supervisorsRefs: Seq[ActorRef])
case class RemoveSupervisors(supervisorsRefs: Seq[ActorRef])
case object GetKnownSupervisors
case object GetStatActors

/* data */
case class KnownSupervisors(supervisorsRefs: Seq[ActorRef])
case class StatActors(me: ActorRef, statActorsRefs: Seq[ActorRef])


class BaseSupervisor extends Actor with ActorLogging {
  val localStatActors = mutable.Set[ActorRef]()
  val supervisorsActors = mutable.Set[ActorRef]()
  var beatDelay = 2.seconds  // TODO: dynamically change
  var nextIndex = 0
  val cluster = Cluster(context.system)

  import context.dispatcher

  override def receive: Receive = LoggingReceive {
    handlerSupervisorEvents orElse
    handlerStatActorsEvents orElse
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

    case AddSupervisors(actors: Seq[ActorRef]) =>
      val newActors = actors.toSet -- supervisorsActors
      supervisorsActors ++= newActors
      log.info(s"Add supervisors ${describeActors(newActors.toSeq)}, set size: ${supervisorsActors.size}")
      newActors.foreach(_ ! GetStatActors)

    case RemoveSupervisors(actors) =>
      supervisorsActors --= actors
      log.info(s"Remove supervisors ${describeActors(actors)}, set size: ${supervisorsActors.size}")

    case GetKnownSupervisors =>
      sender ! KnownSupervisors(supervisorsActors.toSeq)

  }

  protected def handlerStatActorsEvents: Receive = {
    case GetStatActors => sender() ! StatActors(self, localStatActors.toSeq)

    case StatActors(supervisor: ActorRef, statActors: Seq[ActorRef]) =>
      if (!supervisorsActors.contains(supervisor)) {
        log.error(s"Got stat actors from unknown supervisor $supervisor, ignore them")
      } else {
        log.info(s"Got ${statActors.size} new stat actors from $supervisor")
        statActors foreach { _ ! AddSiblings(localStatActors.toSeq) }
      }
  }

  protected def startUp(amount: Int): Unit = {
    for (index <- 0 until amount) {
      addLocalStatActor()
    }
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent],
      classOf[MemberUp], classOf[MemberExited], classOf[MemberRemoved], classOf[ReachableMember],
      classOf[UnreachableMember])
  }

  private def addLocalStatActor(): ActorRef = {
    val newActorRef = context.actorOf(Props(new Stat(beatDelay)), s"stat-$nextIndex")
    nextIndex += 1
    newActorRef ! AddSiblings(localStatActors.toSeq)
    localStatActors.foreach { _ ! AddSiblings(Seq(newActorRef)) }
    localStatActors += newActorRef
    newActorRef
  }
}
