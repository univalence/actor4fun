package actor4fun.internal

import actor4fun.{Actor, ActorRef, ActorSystem}
import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class RemoteActorSystemTest
    extends AnyFunSuiteLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  import scala.concurrent.ExecutionContext.Implicits._

  val system1Port: Int = newPort
  val system2Port: Int = newPort

  val system1: RemoteActorSystem =
    ActorSystem.createRemote("test-actor-system-1", "127.0.0.1", system1Port)
  val system2: RemoteActorSystem =
    ActorSystem.createRemote("test-actor-system-2", "127.0.0.1", system2Port)

  override protected def afterAll(): Unit = {
    system1.shutdown()
    system2.shutdown()
  }
  override protected def afterEach(): Unit = {
    system1.clearActors()
    system2.clearActors()
  }

  test("should find actor on remote if registered") {
    system2.registerAndManage("test", new TestActor)

    val ref = system1.findActorForName(s"actor://127.0.0.1:$system2Port/test")

    assert(ref.isDefined)
  }

  test("should not find actor on remote if not registered") {
    val ref = system1.findActorForName(s"actor://127.0.0.1:$system2Port/test")

    assert(ref.isEmpty)
  }

  test("should communicate remotely") {
    val endActor   = new EndActor
    val end        = system1.registerAndManage("end", endActor)
    val step2Actor = new StepActor(end)
    val step2      = system1.registerAndManage("step-2", step2Actor)
    val step1Actor = new StepActor(step2)
    val step1      = system1.registerAndManage("step-1", step1Actor)

    step1.sendFrom(null, Message(1))

    endActor.lock.await(1000, TimeUnit.MILLISECONDS)

    assert(step1Actor.step === 1)
    assert(step2Actor.step === 2)
    assert(endActor.step === 3)
  }

  test("should get an error when sending message to actor by remote system") {
    val remoteRef = RemoteActorRef.fromURI(s"actor://127.0.0.1:$system2Port/whatever", system1).get
    val test = system1.registerAndManage("test", new TestActor)

    intercept[IllegalArgumentException] {
      remoteRef.sendFrom(test, "hello")
    }
  }

}

class StepActor(next: ActorRef) extends Actor {
  var step: Int = -1

  override def receive(sender: ActorRef)(implicit self: ActorRef): Receive = {
    case Message(step) =>
      this.step = step
      next ! Message(step + 1)
  }
}

class EndActor extends Actor {
  val lock      = new CountDownLatch(1)
  var step: Int = -1

  override def receive(sender: ActorRef)(implicit self: ActorRef): Receive = {
    case Message(step) =>
      this.step = step
      lock.countDown()
  }
}

case class Message(step: Int)
