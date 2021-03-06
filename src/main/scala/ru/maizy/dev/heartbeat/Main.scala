package ru.maizy.dev.heartbeat

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */

import java.util.concurrent.atomic.AtomicBoolean
import sun.misc.SignalHandler
import sun.misc.Signal
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.cluster.Cluster
import akka.actor.ActorSystem
import akka.util.Timeout


object Main extends App with SignalHandler {

  val SIGING = "INT"
  val SIGTERM = "TERM"

  val terminated = new AtomicBoolean(false)
  val UNKNOWN_TERMINATION = 2
  val OPTIONS_ERROR_TERMINATION = 3
  val SUCCESS_TERMINATION = 0

  var actorSystem: Option[ActorSystem] = None

  Signal.handle(new Signal(SIGING), this)
  Signal.handle(new Signal(SIGTERM), this)

  OptionParser.parse(args) match {
    case None => System.exit(OPTIONS_ERROR_TERMINATION)

    case Some(options) =>

      // TODO: do without string parsing
      var additionalConfig = s"""
        akka.remote.netty.tcp = {
          port=${options.port}
          hostname=${options.host}
        }
        """

      options.mode match {
        case Modes.Emulator =>
          println("Force using akka.provider = akka.actor.LocalActorRefProvider")
          additionalConfig += "akka.provider = \"akka.actor.LocalActorRefProvider\""

        case Modes.Production =>
          additionalConfig += "akka.cluster.roles = [\""+ options.role +"\"]"
      }

      val config = ConfigFactory.parseString(additionalConfig).withFallback(ConfigFactory.load())

      implicit val system = ActorSystem("main", config)
      actorSystem = Some(system)
      val logger = system.log
      implicit val executionContext = system.dispatcher
      implicit val defaultTimeout = Timeout(500.millis)  // TODO: from config

      options.mode match {
        case Modes.Emulator =>
          system.log.info("Start in emulator mode")
          EventEmulator.emulate(options.program.get)  // program always exits for that comand
        case Modes.Production =>
          system.log.info("Start in production mode")
          val cluster = Cluster(system)
          var roleHandler: Option[role.RoleHandler] = None
          cluster.selfRoles.foreach {
            case "stat" =>
              roleHandler = Some(new role.Stat(options.statsByNode))
          }

          roleHandler.foreach { h =>
            h.startUp(system, cluster)
          }

        case _ =>
          logger.error("Unsupported role, exiting")
          system.shutdown()
      }

      system.registerOnTermination {
        System.exit(SUCCESS_TERMINATION)
      }
  }

  override def handle(signal: Signal): Unit = {
    actorSystem foreach {_.log.info(s"recieve signal ${signal.getName}")}
    if (terminated.compareAndSet(false, true)) {
      actorSystem foreach { system =>
        if (Seq(SIGING, SIGTERM).contains(signal.getName)) {
          actorSystem foreach {_.log.debug(s"handle signal")}
          system.shutdown()
        }
      }
    } else {
      actorSystem foreach {_.log.info(s"ever terminated, skip")}
    }
  }
}
