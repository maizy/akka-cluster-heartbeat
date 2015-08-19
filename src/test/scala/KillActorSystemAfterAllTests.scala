/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
import scala.concurrent.duration._
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Suite}

trait KillActorSystemAfterAllTests extends BeforeAndAfterAll {

  this: TestKit with Suite =>

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.shutdown()
    system.awaitTermination(10.seconds)
  }
}
