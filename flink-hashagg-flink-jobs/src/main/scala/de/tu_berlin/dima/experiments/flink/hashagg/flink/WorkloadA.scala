package de.tu_berlin.dima.experiments.flink.hashagg.flink

import org.apache.flink.api.common.operators.base.ReduceOperatorBase.CombineHint
import org.apache.flink.api.scala._

/** Compute the largest length per group. */
object WorkloadA {

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
      .map{kv => (kv._1, kv._2.length)}
      .groupBy(0)
      .reduce((x, y) => (x._1, Math.max(x._2, y._2)), combineHint)
      .writeAsCsv(outputPath)

    env.execute("WorkloadA")
  }

}
