package com.github.yoshiyoshifujii.sample

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.dynamodbv2.streamsadapter.{ AmazonDynamoDBStreamsAdapterClient, StreamsWorkerFactory }
import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDB, AmazonDynamoDBStreams }
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{ KinesisClientLibConfiguration, Worker }
import com.github.yoshiyoshifujii.sample.Main.StoppedReply

object StreamsAdapterDemo {
  val name: String = "StreamAdapterDemo"

  sealed trait Command
  case object Start                                extends Command
  case class Stop(replyTo: ActorRef[StoppedReply]) extends Command

  def apply(
      dynamoDBClient: AmazonDynamoDB,
      cloudWatchClient: AmazonCloudWatch,
      dynamoDBStreamsClient: AmazonDynamoDBStreams,
      adapterClient: AmazonDynamoDBStreamsAdapterClient,
      workerConfig: KinesisClientLibConfiguration
  ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      var worker: Worker = null
      var thread: Thread = null
      Behaviors.receiveMessage {
        case Start =>
          ctx.log.info("Streams Adapter Start")

          val recordProcessorFactory = new StreamsRecordProcessorFactory(ctx)

          worker = StreamsWorkerFactory.createDynamoDbStreamsWorker(recordProcessorFactory,
                                                                    workerConfig,
                                                                    adapterClient,
                                                                    dynamoDBClient,
                                                                    cloudWatchClient)
          thread = new Thread(worker)
          thread.start()
          Behaviors.same
        case Stop(replyTo) =>
          println("Streams Adapter Stop")
          worker.startGracefulShutdown().get()
          replyTo ! StoppedReply
          Behaviors.stopped
      }
    }

}
