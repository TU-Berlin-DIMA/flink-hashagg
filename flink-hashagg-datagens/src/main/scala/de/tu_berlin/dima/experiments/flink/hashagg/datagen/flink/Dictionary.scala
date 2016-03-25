package de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink

import de.tu_berlin.dima.experiments.flink.hashagg.datagen.util.RanHash

class Dictionary (seed : Long, size : Int) extends Iterable[String] {

  import Dictionary._

  private val random : RanHash = new RanHash(seed)

  override def iterator: Iterator[String] = words().iterator

  def word (index : Int) : String = {
    require(0 <= index && index < size)
    // skip to the correct position within the random sequence
    random.skipTo(index * MAX_LENGTH)
    val len = random.nextInt(MAX_LENGTH - MIN_LENGTH - 1) + MIN_LENGTH
    val strBld = new StringBuilder(len)
    for (i <- 0 until len) {
        val c = ('a'.toInt + random.nextInt(NUM_CHARACTERS)).toChar
        strBld.append(c)
      }
    strBld.mkString
  }

  def words () : Array[String] = (0 until size).map(i => word(i)).toArray
}

object Dictionary {
  val MIN_LENGTH = 10
  val MAX_LENGTH = 20
  val NUM_CHARACTERS = 26
}
