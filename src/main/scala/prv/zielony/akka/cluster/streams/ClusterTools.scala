package prv.zielony.akka.cluster.streams

import akka.actor.{ActorSystem, Address}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory

import scala.collection.immutable.Seq

trait ClusterTools {

  def inCluster(systemName: String, seedNodes: Seq[Address], port: Int)(f: Cluster => Unit): Address = {
    implicit val actorSystem = ActorSystem(
      systemName,
      ConfigFactory.defaultApplication().withFallback(ConfigFactory.parseString(defaultConfig(port)))
    )
    val cluster = Cluster(actorSystem)
    cluster.joinSeedNodes(seedNodes)
    cluster.registerOnMemberUp(f(cluster))

    Address("akka.tcp", systemName, "127.0.0.1", port)
  }

  // TODO: Correct settings?
  def defaultConfig(port: Int): String = s"""akka{
        actor.provider = "cluster"

        remote.netty.tcp {
          hostname = "127.0.0.1"
          port = $port
        }

       }""".stripMargin
}
