/**
 * Copyright (C) ${project.inceptionYear} TU Berlin (alexander.alexandrov@tu-berlin.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  import CombinerMetricsExtractor.{LogEntry, CombinerMetricPattern}

  final def receive: Receive = {
    case Line(LogEntry(_, CombinerMetricPattern(taskName, number, total, metricName, value))) =>
      writer ! ExperimentEvent(
        experimentRunID /**/ = run.id,
        name /*           */ = Symbol(metricName),
        task /*           */ = Some(taskName),
        taskInstance /*   */ = Some(number.toInt),
        vLong /*          */ = Some(value.toLong))
  }
}

/** Companion object. */
@Component
object CombinerMetricsExtractor extends EventExtractorCompanion with PatternBasedProcessMatching {

  val TMLog = """flink-(.+)-taskmanager-\d+-(.+)\.log""".r
  val LogEntry = """([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}).* - (.+)""".r
  val CombinerMetricPattern = """(.+) \((\d+)/(\d+)\) (\w+): (\d+)""".r

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

