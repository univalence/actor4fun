package actor4fun.internal

import actor4fun.internal.RemoteActorSystem.serialize
import actor4fun.internal.network.{
  ActorEndPointGrpc,
  NetActorMessage,
  NetActorRef
}
import actor4fun.{ActorMessage, ActorRef, ActorSystem}
import com.google.protobuf.ByteString
import java.net.URI
import org.slf4j.{Logger, LoggerFactory}
import scala.util.Try

/**
 * Reference to an actor with remote communication capability.
 *
 * The reference contains two actor systems:
 *  - a reference to the actor system managing the referenced actor
 *  (it may be local or remote),
 *  - the local actor system that has created this reference.
 *
 *  If the reference to the actor system is equivalent the local actor
 *  system, them this reference is a reference to a local. If not,
 *  them this reference is a reference to a remote actor.
 *
 * @param name name of the actor
 * @param actorSystemRef reference to the actor system managing the
 *                       actor
 * @param actorSystem actor system managing this reference
 */
case class RemoteActorRef(
    override val name: String,
    actorSystemRef: RemoteActorSystemRef,
    actorSystem: RemoteActorSystem
) extends ActorRef {

  val logger: Logger = LoggerFactory.getLogger(s"$name-ref")

  val isLocal: Boolean  = actorSystemRef == actorSystem.self
  val isRemote: Boolean = !isLocal

  /** @inheritdoc */
  override def sendFrom(sender: ActorRef, message: Any): Unit = {
    logger.debug(s"sending $message to $sender")
    this match {
      case ref @ RemoteActorRef(name, _, system) if ref.isLocal =>
        localSendFrom(sender, message, name, system)

      case ref @ RemoteActorRef(_, receiverSysRef, _) if ref.isRemote =>
        sender match {
          case RemoteActorRef(_, senderSysRef, system) =>
            remoteSendFrom(
              sender,
              message,
              receiverSysRef,
              senderSysRef,
              system
            )

          case _ =>
            throw new IllegalArgumentException(
              s"unmanaged sender reference $sender"
            )
        }

      case _ =>
        throw new IllegalArgumentException(
          s"unmanaged receiver reference $this"
        )
    }
  }

  private def localSendFrom(
      sender: ActorRef,
      message: Any,
      name: String,
      system: RemoteActorSystem
  ): Unit = {
    system
      .actors(system.refs(name))
      .pushMessage(ActorMessage(sender, message))
  }

  private def remoteSendFrom(
      sender: ActorRef,
      message: Any,
      receiverSysRef: RemoteActorSystemRef,
      senderSysRef: RemoteActorSystemRef,
      managingSystem: RemoteActorSystem
  ): Unit = {
    val senderRef = NetActorRef(
      senderSysRef.host,
      senderSysRef.port,
      sender.name
    )
    val receiverRef = NetActorRef(
      receiverSysRef.host,
      receiverSysRef.port,
      this.name
    )

    val channel =
      managingSystem.connect(receiverSysRef.host, receiverSysRef.port)
    try {
      val payload = ByteString.copyFrom(serialize(message))

      val ack =
        ActorEndPointGrpc
          .blockingStub(channel)
          .receive(
            NetActorMessage(
              sender   = Option(senderRef),
              receiver = Option(receiverRef),
              payload  = payload
            )
          )

      if (!ack.isOk)
        throw new IllegalArgumentException(
          s"error from remote actor system: ${ack.error}"
        )
    } finally {
      channel.shutdown()
    }
  }
}
object RemoteActorRef {
  val uriActorScheme = "actor"

  /**
   * Try to get actor name and actor system reference from a URI.
   *
   * @param uriStr string representing a URI
   * @return actor name and actor system reference
   */
  private[internal] def fromURI(
      uriStr: String
  ): Option[(String, RemoteActorSystemRef)] = {
    for {
      uri    <- Try { URI.create(uriStr) }.toOption
      scheme <- Option(uri.getScheme)
      if scheme == uriActorScheme
      host <- Option(uri.getHost)
      port <- Option(uri.getPort).filter(_ >= 0)
      path <- Option(uri.getPath)
    } yield {
      val name = if (path.startsWith("/")) path.tail else path

      (name, RemoteActorSystemRef(host, port))
    }
  }

  def fromURI(uriStr: String, system: ActorSystem): Option[RemoteActorRef] =
    system match {
      case _: LocalActorSystem => None
      case remoteSys: RemoteActorSystem =>
        for ((name, sysRef) <- fromURI(uriStr))
          yield RemoteActorRef(name, sysRef, remoteSys)
    }
}
