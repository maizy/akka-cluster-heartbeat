package ru.maizy.dev.heartbeat

import java.util.concurrent.atomic.AtomicBoolean

import sun.misc.SignalHandler
import sun.misc.Signal

import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory
import akka.cluster.Cluster
import akka.actor.ActorSystem
import akka.util.Timeout

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
object Main extends App with SignalHandler {

  /* USAGE:

    sbt assembly
    # hook version:
    java -jar target/scala-2.11/akka-cluster-heartbeat-assembly-0.1.jar hook
    # signal version:
    java -jar  target/scala-2.11/akka-cluster-heartbeat-assembly-0.1.jar signal

    # then in other shell session
    # find PID, ex 12345
    jps -m | grep akka

    # send signals
    kill -TERM 12345
    # or
    kill -INT 12345

    # back to shell with java ..., get exit code value
    echo $?
   */

  val SIGING = "INT"
  val SIGTERM = "TERM"

  val mode = args.toList match {
    case x :: _ => x
    case _ => "signal"
  }

  println(s"Mode: $mode")

  if (mode == "signal") {
    println("register signal handlers")
    Signal.handle(new Signal(SIGING), this)
    Signal.handle(new Signal(SIGTERM), this)
  }

  val config = ConfigFactory.load()

  val system = ActorSystem("main", config)
  val logger = system.log
  implicit val executionContext = system.dispatcher
  implicit val defaultTimeout = Timeout(500.millis)
  val cluster = Cluster(system)

  //akka hook
  system.registerOnTermination {
    println("akka system terminated, return exit code = 0")
    System.exit(0)
  }

  val terminated = new AtomicBoolean(false)

  // вариант 1
  if (mode == "hook") {
    scala.sys.addShutdownHook {
      if (terminated.compareAndSet(false, true)) {
        println("Shutdown hook")
        system.shutdown() // это работает, ActorSystem аккуратно тормозит, но не ставиться exit code = 0
        // System.exit(0) // это тоже не работает о_0
                          // exit code после sigint = 130, sigterm = 146
      } else {
        println("Terminated before")
      }
    }
  }


  // вариант 2
  override def handle(signal: Signal): Unit = {
    println(s"Signal received - ${signal.getName}")
    if (!terminated.get()) {
      println(s"Signal processed - ${signal.getName}")
      if (terminated.compareAndSet(false, true) && List(SIGING, SIGTERM).contains(signal.getName)) {
        println("call system.shutdown()")
        system.shutdown()
        // System.exit(2) // так можно, но system.shutdown тоже верно отработает
                          // и вызовет registerOnTermination, который и сам поставит exit code
      }
    } else {
      println("Terminated before")
    }
  }
}
