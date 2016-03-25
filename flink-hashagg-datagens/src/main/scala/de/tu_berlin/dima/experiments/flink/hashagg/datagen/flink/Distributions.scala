package de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink

import org.apache.commons.math3.distribution._

object Distributions {

  trait DiscreteDistribution {
    def sample(cumulativeProbability: Double) : Int
  }

  case class DiscreteUniform(k: Int) extends DiscreteDistribution {
    val distribution = new UniformIntegerDistribution(0, k - 1)
    def sample(cp: Double) = distribution.inverseCumulativeProbability(cp)
  }

  // approximated by NormalDistribution, since BinomialDistribution is too slow
  // for the relationship between the two, see this article
  // http://www.real-statistics.com/binomial-and-related-distributions/relationship-binomial-and-normal-distributions/
  case class Binomial(sampleSize: Int, p: Double) extends DiscreteDistribution {
    val n = sampleSize - 1
    val distribution = new NormalDistribution(n * p, Math.sqrt(n * p * (1 - p)))
    def sample(cp: Double) = (distribution.inverseCumulativeProbability(cp) - 1).toInt % n
  }

  // TODO: since BinomialDistribution is too slow, either approximate with ParetoDistribution or write custom CDF^{-1}
  // for the relationship between the two, see this article
  // http://www.hpl.hp.com/research/idl/papers/ranking/ranking.html
  case class Zipf(sampleSize: Int, exponent: Double) extends DiscreteDistribution {
    val n = sampleSize
    val distribution = new ParetoDistribution(1, 1 + 1/exponent)
    def sample(cp: Double) = (distribution.inverseCumulativeProbability(cp) - 1).toInt % n
  }
}