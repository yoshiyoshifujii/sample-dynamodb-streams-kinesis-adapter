package com.github.yoshiyoshifujii.sample

import akka.actor.typed.scaladsl.ActorContext
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.{ InitializationInput, ProcessRecordsInput, ShutdownInput }

class StreamRecordProcessor(ctx: ActorContext[_]) extends IRecordProcessor {

  override def initialize(initializationInput: InitializationInput): Unit = {
    ctx.log.info(s"initialize($initializationInput)")
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    ctx.log.info(s">>> processRecords($processRecordsInput)")
  }

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    if (shutdownInput.getShutdownReason == ShutdownReason.TERMINATE) {
      try {
        shutdownInput.getCheckpointer.checkpoint()
      } catch {
        case e: Exception =>
          ctx.log.error("StreamRecordProcessor error", e)
      }
    }
  }
}
