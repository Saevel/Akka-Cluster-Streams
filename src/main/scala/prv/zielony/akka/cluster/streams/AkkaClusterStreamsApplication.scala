package prv.zielony.akka.cluster.streams

import akka.actor.{ActorPath, ActorRef, Address, PoisonPill, Props}
import akka.cluster.client.{ClusterClient, ClusterClientReceptionist, ClusterClientSettings}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.actor.ActorSubscriber
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Sink, Source}

import scala.collection.immutable.Seq

object AkkaClusterStreamsApplication extends App with ClusterTools {

  val systemName = "AkkaClusterStreams"

  val nodeOne = inCluster(systemName, Seq(Address("akka.tcp", systemName, "127.0.0.1", 5575)), 5575) { cluster =>

    implicit val actorSystem = cluster.system
    implicit val materializer: Materializer = ActorMaterializer()

    val publisherRef = cluster.system.actorOf(Props[ReceiverActor], "receiver")

    ClusterClientReceptionist(cluster.system).registerService(publisherRef)

    Source.fromPublisher(ActorPublisher[Int](publisherRef))
      .runForeach(x => println(s"Output stream processed: $x"))
  }

  Thread.sleep(3 * 1000)

  val nodeTwo = inCluster(systemName, Seq(nodeOne), 5576){ cluster =>

    implicit val actorSystem = cluster.system
    implicit val materializer: Materializer = ActorMaterializer()

    lazy val clusterClient: ActorRef = cluster.system.actorOf(ClusterClient.props(
      ClusterClientSettings(cluster.system).withInitialContacts(Set(onNode(nodeOne)))), "client")

    lazy val subscriberRef: ActorRef = cluster.system.actorOf(Props(new SenderActor[Int](clusterClient)), "sender")

    ClusterClientReceptionist(cluster.system).registerService(subscriberRef)

    Thread.sleep(3 * 1000)

    Source.fromIterator(() => (0 to 10).iterator)
      .alsoTo(Sink.foreach(x => println(s"Input stream processed: $x")))
      .runWith(Sink.actorRef[Int](subscriberRef, PoisonPill))
  }

  private def onNode(node: Address): ActorPath = ActorPath.fromString(
    s"${node.protocol}://$systemName@${node.host.get}:${node.port.get}/system/receptionist"
  )
}
