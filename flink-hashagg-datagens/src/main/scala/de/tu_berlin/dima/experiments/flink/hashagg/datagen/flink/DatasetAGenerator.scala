package de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink

import de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink.Distributions.{Binomial, DiscreteDistribution, DiscreteUniform, Zipf}
import de.tu_berlin.dima.experiments.flink.hashagg.datagen.util.RanHash
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem
import org.apache.flink.util.NumberSequenceIterator

object DatasetAGenerator {

  val SEED = 0xC00FFEE
  val SIZE_OF_DICTIONARY = 1000000

  val valDistribution = DiscreteUniform(SIZE_OF_DICTIONARY)

  def main(args: Array[String]): Unit = {

    if (args.length != 5) {
      Console.err.println("Usage: <jar> numberOfTasks tuplesPerTask key-cardinality key-distribution[params] outputPath")
      System.exit(-1)
    }

    val numberOfTasks   = args(0).toInt
    val tuplesPerTask   = args(1).toLong
    val keyCardinality  = args(2).toInt
    val keyDistribution = parseDist(keyCardinality, args(3))
    val outputPath      = args(4)

    val numberOfPairs   = numberOfTasks * tuplesPerTask

    val environment = ExecutionEnvironment.getExecutionEnvironment

    environment
      // create a sequence [1 .. N] to create N words
      .fromParallelCollection(new NumberSequenceIterator(1, numberOfPairs))
      // set up workers
      .setParallelism(numberOfTasks)
      // map every n <- [1 .. N] to a random word sampled from a word list and a key
      .map(i => {
        val r = new RanHash(SEED + SIZE_OF_DICTIONARY * Dictionary.MAX_LENGTH + 2 * i)
        val k = keyDistribution.sample(r.next())
        val v = new Dictionary(SEED, SIZE_OF_DICTIONARY).word(valDistribution.sample(r.next()))
        s"$k,$v"
      })
      // write result to file
      .writeAsText(outputPath, FileSystem.WriteMode.OVERWRITE)

    environment.execute(s"KeyValuePairsGenerator[$numberOfPairs][$keyCardinality][${args(3)}]")
  }

  object Patterns {
    val DiscreteUniform = """(Uniform)""".r
    val Binomial = """Binomial\[(1|1\.0|0\.\d+)\]""".r
    val Zipf = """Zipf\[(\d+(?:\.\d+)?)\]""".r
  }

  def parseDist(card: Int, s: String): DiscreteDistribution = s match {
    case Patterns.DiscreteUniform(_) =>
      DiscreteUniform(card)
    case Patterns.Binomial(a) =>
      Binomial(card, a.toDouble)
    case Patterns.Zipf(a) =>
      Zipf(card, a.toDouble)
  }

}
