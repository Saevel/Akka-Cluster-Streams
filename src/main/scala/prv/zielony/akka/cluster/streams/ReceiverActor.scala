package prv.zielony.akka.cluster.streams

import akka.actor.Actor
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.impl.fusing.ActorGraphInterpreter.ExposedPublisher

import scala.collection.mutable

class ReceiverActor extends ActorPublisher[Int] {

  var queue: scala.collection.mutable.Queue[Int] = mutable.Queue()

  override def receive: Actor.Receive = {
    case i: Int => queue.enqueue(i)
      publishIfNeeded
    case Request(cnt) =>
      publishIfNeeded
    case Cancel => context.stop(self)
    case _ =>
  }

  def publishIfNeeded = {
    while(queue.nonEmpty && isActive && totalDemand > 0) {
      println("ReceiverActor publishing message to stream")
      onNext(queue.dequeue())
    }
  }
}
