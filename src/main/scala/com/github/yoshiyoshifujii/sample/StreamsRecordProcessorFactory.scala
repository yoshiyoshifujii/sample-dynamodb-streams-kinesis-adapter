package com.github.yoshiyoshifujii.sample

import akka.actor.typed.scaladsl.ActorContext
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{ IRecordProcessor, IRecordProcessorFactory }

class StreamsRecordProcessorFactory(ctx: ActorContext[_]) extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = new StreamRecordProcessor(ctx)
}
