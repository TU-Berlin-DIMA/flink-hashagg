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
package de.tu_berlin.dima.experiments.flink.hashagg.flink

import org.apache.flink.api.common.operators.base.ReduceOperatorBase.CombineHint
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem

/** Get the largest value per group based on lexicographic ordering. */
object WorkloadY {

  def main(args: Array[String]) {
    if (args.length != 3) {
      Console.err.println("Usage: <jar> combiner-strategy inputPath outputPath")
      System.exit(-1)
    }

    val combineHint = args(0).toLowerCase match {
      case "hash" =>
        CombineHint.HASH
      case "sort" =>
        CombineHint.SORT
      case _ =>
        CombineHint.OPTIMIZER_CHOOSES
    }
    val inputPath = args(1)
    val outputPath = args(2)

    val env = ExecutionEnvironment.getExecutionEnvironment

    env
      .readCsvFile[(Long, String)](inputPath)
      .groupBy(0)
      .reduce((x, y) => (x._1, if (x._2 > y._2) x._2 else y._2), combineHint)
      .writeAsCsv(outputPath, writeMode = FileSystem.WriteMode.OVERWRITE)

    env.execute(s"WorkloadY[${combineHint.toString.toLowerCase}]")
  }

}
