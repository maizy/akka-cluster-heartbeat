package ru.maizy.dev.heartbeat

import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.util.Timeout

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
object Main extends App {
  OptionParser.parse(args) match {

    case None => System.exit(2)

    case Some(options) =>
      var additionalConfig = s"""
         akka.remote.netty.tcp = {
           port=${options.port}
           hostname=${options.host}
         }
        """

      if (options.mode == Modes.Emulator) {
        println("Force using akka.provider = akka.actor.LocalActorRefProvider")
        additionalConfig += "akka.provider = \"akka.actor.LocalActorRefProvider\""
      }

      val config = ConfigFactory.parseString(additionalConfig).withFallback(ConfigFactory.load())

      implicit val system = ActorSystem("main", config)
      implicit val executionContext = system.dispatcher
      implicit val defaultTimeout = Timeout(500.millis) //TODO: from config

      options.mode match {
        case Modes.Emulator => EventEmulator.emulate(options.program.get)  //program always exits for that comand
        case Modes.Production => println("start here!")
        case _ => System.exit(1)
      }



  }
}
