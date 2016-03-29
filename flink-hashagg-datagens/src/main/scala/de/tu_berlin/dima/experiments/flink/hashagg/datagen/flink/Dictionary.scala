package de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink

import de.tu_berlin.dima.experiments.flink.hashagg.datagen.util.RanHash

class Dictionary private(seed: Long, size: Int) {

  import Dictionary._

  private val random: RanHash = new RanHash(seed)

  def apply(index: Int): String = {
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
}

object Dictionary {

  val MIN_LENGTH /*    */ = 10
  val MAX_LENGTH /*    */ = 20
  val NUM_CHARACTERS /**/ = 26

  def apply(seed: Long, size: Int): Dictionary =
    new Dictionary(seed, size)
}
