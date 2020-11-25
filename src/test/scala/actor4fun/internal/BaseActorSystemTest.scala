package actor4fun.internal

import actor4fun.ActorSystem
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class BaseActorSystemTest
    extends AnyFunSuiteLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  import scala.concurrent.ExecutionContext.Implicits._

  val system: LocalActorSystem = ActorSystem.createLocal("test-actor-system")

  override protected def afterAll(): Unit  = system.shutdown()
  override protected def afterEach(): Unit = system.clearActors()

  test("should not find an unregistered actor") {
    assert(system.findActorForName("test").isEmpty)
  }

  test("should register and find an actor") {
    val actor = new TestActor
    system.registerAndManage("test", actor)

    assert(system.findActorForName("test").isDefined)
  }

  test("should registering an actor will start it") {
    val actor = new TestActor
    system.registerAndManage("test", actor)

    actor.waitForStart(1000)

    assert(actor.hasStarted)
  }

  test("should unregister a registered actor") {
    val actor = new TestActor
    val ref = system.registerAndManage("test", actor)
    system.unregisterAndStop(ref)

    assert(system.findActorForName("test").isEmpty)
  }

  test("should unregister an actor will shut down it") {
    val actor = new TestActor
    val ref = system.registerAndManage("test", actor)
    system.unregisterAndStop(ref)

    actor.waitForShutdown(2000)

    assert(actor.hasShutdown)
  }

  test("should send exception when unregistering twice the same actor") {
    intercept[NoSuchElementException] {
      val ref = system.registerAndManage("test", new TestActor)
      system.unregisterAndStop(ref)

      system.unregisterAndStop(ref)
    }
  }

  test("should get all actor names") {
    system.registerAndManage("test-1", new TestActor)
    system.registerAndManage("test-2", new TestActor)

    assert(system.actorNames === Set("test-1", "test-2"))
  }

  test("should schedule a task") {
    val actor = new TestActor
    val ref = system.registerAndManage("test-1", actor)

    val task = system.schedule(10, 100) {
      () => ref.sendFrom(null, "message")
    }

    try {
      Thread.sleep(500)

      assert(actor.messages.size === 5)
      assert(actor.messages.forall(_ === "message"))
    } finally {
      task.shutdown()
    }
  }

}

