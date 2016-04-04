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
package de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink

import de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink.Distributions.{Binomial, DiscreteDistribution, DiscreteUniform, Zipf}
import de.tu_berlin.dima.experiments.flink.hashagg.datagen.util.RanHash
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem
import org.apache.flink.util.NumberSequenceIterator

/** Data generator for `Dataset A`.
  *
  * The generator maps a PRNG string to a pseudo-random `(key, value)` sequence as follows.
  *
  * The first fragment of size `SIZE_OF_DICT * Dictionary.MAX_LENGTH` is mapped to the finite values dictionary.
  * The second fragment of size `numberOfPairs * 2` is mapped to the actual `(key, value)` pairs.
  *
  * Each pair thereby consumes exactly two PRNG numbers - the first is used to sample the key, and the second the value.
  */
object DatasetAGenerator {

  val SIZE_OF_DICT /**/ = 1000000

  val SEED_ROOT /*   */ = 0xC00FFEE
  val SEED_DICT /*   */ = SEED_ROOT
  val SEED_PAIR /*   */ = SEED_ROOT + SIZE_OF_DICT * Dictionary.MAX_LENGTH

  val valDist /*     */ = DiscreteUniform(SIZE_OF_DICT)

  def main(args: Array[String]): Unit = {

    if (args.length != 5) {
      Console.err.println("Usage: <jar> numberOfTasks tuplesPerTask keyCardinality keyDist[params] outputPath")
      System.exit(-1)
    }

    val numberOfTasks /*  */ = args(0).toInt
    val tuplesPerTask /*  */ = args(1).toLong
    val keyCardinality /* */ = args(2).toInt
    val keyDist /*        */ = parseDist(keyCardinality, args(3))
    val outputPath /*     */ = args(4)

    val numberOfPairs /*  */ = numberOfTasks * tuplesPerTask

    val environment /*    */ = ExecutionEnvironment.getExecutionEnvironment

    environment
      // create a sequence [1 .. N] to create N words
      .fromParallelCollection(new NumberSequenceIterator(1, numberOfPairs))
      // set up workers
      .setParallelism(numberOfTasks)
      // map every n <- [1 .. N] to a random word from the dictionary and a random key
      .map(i => {
        val r = new RanHash(SEED_PAIR + i * 2)
        val k = keyDist.sample(r.next())
        val v = Dictionary(SEED_DICT, SIZE_OF_DICT)(valDist.sample(r.next()))
        s"$k,$v"
      })
      // write result to file
      .writeAsText(outputPath, FileSystem.WriteMode.OVERWRITE)

    environment.execute(s"DatasetAGenerator[E=$tuplesPerTask][K=$keyCardinality][P=${args(3)}]")
  }

  object Patterns {
    val DiscreteUniform = """(Uniform)""".r
    val Binomial /*  */ = """Binomial\[(1|1\.0|0\.\d+)\]""".r
    val Zipf /*      */ = """Zipf\[(\d+(?:\.\d+)?)\]""".r
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
