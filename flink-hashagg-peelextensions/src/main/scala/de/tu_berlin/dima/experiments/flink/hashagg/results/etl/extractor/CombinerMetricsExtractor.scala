package de.tu_berlin.dima.experiments.flink.hashagg.results.etl.extractor

import java.io.File
import java.nio.file.Paths

import akka.actor.{ActorRef, FSM, Props}
import org.peelframework.core.results.etl.extractor.{EventExtractor, EventExtractorCompanion, PatternBasedProcessMatching}
import org.peelframework.core.results.etl.reader.{FileReader, Line, LineFileReader}
import org.peelframework.core.results.model.{ExperimentEvent, ExperimentRun}
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import scala.util.matching.Regex

/** An extractor for Flink task transition events. */
class CombinerMetricsExtractor(
  override val run: ExperimentRun,
  override val appContext: ApplicationContext,
  override val writer: ActorRef) extends EventExtractor[Line] {

  import CombinerMetricsExtractor.{LogEntry, NumEmissions, NumEmittedRecords}

  final def receive: Receive = {
    case Line(LogEntry(_, NumEmissions(name, number, total, numEmissions))) =>
      writer ! ExperimentEvent(
        experimentRunID /**/ = run.id,
        name /*           */ = 'num_emissions,
        task /*           */ = Some(name),
        taskInstance /*   */ = Some(number.toInt),
        vLong /*          */ = Some(numEmissions.toLong))
    case Line(LogEntry(_, NumEmittedRecords(name, number, total, numEmittedRecords))) =>
      writer ! ExperimentEvent(
        experimentRunID /**/ = run.id,
        name /*           */ = 'num_emitted_records,
        task /*           */ = Some(name),
        taskInstance /*   */ = Some(number.toInt),
        vLong /*          */ = Some(numEmittedRecords.toLong))
  }
}

/** Companion object. */
@Component
object CombinerMetricsExtractor extends EventExtractorCompanion with PatternBasedProcessMatching {

  val TMLog = """flink-(.+)-taskmanager-\d+-(.+)\.log""".r
  val LogEntry = """([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}).* - (.+)""".r
  val NumEmissions = """(.+) \((\d+)/(\d+)\) numEmissions: (\d+)""".r
  val NumEmittedRecords = """(.+) \((\d+)/(\d+)\) numEmittedRecords: (\d+)""".r

  /** A prefix fore the relative file that needs to match. **/
  override val prefix: String = {
    Paths.get("logs", "flink").toString
  }

  /** A list of file patterns for in which the event extractor is interested */
  override val filePatterns: Seq[Regex] = {
    Seq(TMLog)
  }

  /** Constructs a reader that parses the file as a sequence of objects that can be handled by the extractor actor. */
  override def reader(file: File): FileReader[Any] = {
    LineFileReader(file)
  }

  /** Create the extractor props. */
  override def props(run: ExperimentRun, context: ApplicationContext, file: File, writer: ActorRef): Props = {
    Props(new CombinerMetricsExtractor(run, context, writer))
  }
}

