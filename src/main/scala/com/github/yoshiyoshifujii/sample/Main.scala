package com.github.yoshiyoshifujii.sample

import java.util.UUID

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.util.Timeout
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDBClientBuilder, AmazonDynamoDBStreamsClientBuilder }
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{
  InitialPositionInStream,
  KinesisClientLibConfiguration
}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object Main extends App {
  sealed trait StoppedReply
  final case object StoppedReply extends StoppedReply

  object Guardian {
    val name: String = "Main"

    sealed trait Command
    final case class GracefulShutdown(replyTo: ActorRef[StoppedReply]) extends Command

    def apply(): Behavior[Command] = Behaviors.setup[Command] { ctx =>
      val config          = ctx.system.settings.config
      val streamName      = config.getString("stream.arn")
      val applicationName = config.getString("application.name")
      val workerId        = UUID.randomUUID().toString

      val awsRegion           = Regions.AP_NORTHEAST_1
      val credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance()

      val dynamoDBClient = AmazonDynamoDBClientBuilder
        .standard()
        .withRegion(awsRegion)
        .withCredentials(credentialsProvider)
        .build()
      val cloudWatchClient = AmazonCloudWatchClientBuilder
        .standard()
        .withRegion(awsRegion)
        .withCredentials(credentialsProvider)
        .build()
      val dynamoDBStreamsClient = AmazonDynamoDBStreamsClientBuilder
        .standard()
        .withRegion(awsRegion)
        .withCredentials(credentialsProvider)
        .build()
      val adapterClient = new AmazonDynamoDBStreamsAdapterClient(dynamoDBStreamsClient)
      val workerConfig = new KinesisClientLibConfiguration(
        applicationName,
        streamName,
        credentialsProvider,
        workerId
      ).withMaxRecords(1000)
        .withIdleTimeBetweenReadsInMillis(500)
        .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)

      val streamAdapterDemo = StreamsAdapterDemo(
        dynamoDBClient,
        cloudWatchClient,
        dynamoDBStreamsClient,
        adapterClient,
        workerConfig
      )
      val actor = ctx.spawn(streamAdapterDemo, StreamsAdapterDemo.name)
      actor ! StreamsAdapterDemo.Start

      Behaviors.receiveMessage[Command] {
        case GracefulShutdown(replyTo) =>
          actor ! StreamsAdapterDemo.Stop(replyTo)
          Behaviors.stopped
      }
    }
  }

  val config          = ConfigFactory.load()
  implicit val system = ActorSystem[Guardian.Command](Guardian(), Guardian.name, config)

  sys.addShutdownHook {
    import system.executionContext
    implicit val timeout = Timeout(5.seconds)

    val future = Future
      .successful(())
      .flatMap { _ =>
        system.ask(Guardian.GracefulShutdown)
      }
      .flatMap(_ => system.whenTerminated)
    Await.result(future, 5.seconds)
  }

}
