package actor4fun

import java.net.ServerSocket
import java.util
import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean

package object internal {

  class TestActor extends Actor {
    private val inHasStarted  = new AtomicBoolean(false)
    private val inHasShutdown = new AtomicBoolean(false)
    private val inMessages: util.Queue[Any] =
      new util.concurrent.ConcurrentLinkedQueue()
    private val startLock    = new CountDownLatch(1)
    private val shutdownLock = new CountDownLatch(1)

    def hasStarted: Boolean  = inHasStarted.get()
    def hasShutdown: Boolean = inHasShutdown.get()
    def messages: Iterable[Any] =
      scala.collection.JavaConverters.asScala(inMessages)

    def waitForStart(duration: Long): Unit = {
      startLock.await(duration, TimeUnit.MILLISECONDS)
    }

    def waitForShutdown(duration: Long): Unit = {
      shutdownLock.await(duration, TimeUnit.MILLISECONDS)
    }

    override def receive(sender: ActorRef)(implicit self: ActorRef): Receive = {
      case message =>
        inMessages.offer(message)
    }

    override def onStart(implicit self: ActorRef): Unit = synchronized {
      inHasStarted.set(true)
      startLock.countDown()
    }

    override def onShutdown(): Unit = synchronized {
      inHasShutdown.set(true)
      shutdownLock.countDown()
    }
  }

  def newPort: Int = {
    val socket = new ServerSocket(0)
    try {
      socket.getLocalPort
    } finally {
      socket.close()
    }
  }

}
