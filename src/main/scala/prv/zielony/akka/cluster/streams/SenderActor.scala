package prv.zielony.akka.cluster.streams

import akka.actor.{Actor, ActorRef}
import akka.cluster.client.ClusterClient.Send
import akka.stream.actor.ActorSubscriberMessage.OnNext

class SenderActor[T](clusterClient: ActorRef) extends Actor {
  override def receive: Receive = {
    case t: T => {
      println(s"SenderActor sending message: $t")
      clusterClient ! Send("/user/receiver", t, false)
    }
  }
}
