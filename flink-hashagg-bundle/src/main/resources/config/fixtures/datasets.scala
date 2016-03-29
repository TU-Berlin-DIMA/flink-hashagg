package config.fixtures

import com.typesafe.config.ConfigFactory
import org.peelframework.core.beans.data.{CopiedDataSet, DataSet, ExperimentOutput, GeneratedDataSet}
import org.peelframework.core.beans.experiment.ExperimentSequence.SimpleParameters
import org.peelframework.core.beans.experiment.{ExperimentSequence, ExperimentSuite}
import org.peelframework.flink.beans.experiment.FlinkExperiment
import org.peelframework.flink.beans.job.FlinkJob
import org.peelframework.flink.beans.system.Flink
import org.peelframework.hadoop.beans.system.HDFS2
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.context.{ApplicationContext, ApplicationContextAware}

/** `WordCount` experiment fixtures for the 'flink-hashagg' bundle. */
@Configuration
class datasets extends ApplicationContextAware {

  /* The enclosing application context. */
  var ctx: ApplicationContext = null

  def setApplicationContext(ctx: ApplicationContext): Unit = {
    this.ctx = ctx
  }

  // ---------------------------------------------------
  // Data Generators
  // ---------------------------------------------------

  def dg(name: String, tuplesPerTask: Long, keyCardinality: Int, keyDist: String): FlinkJob = new FlinkJob(
    runner /* */ = ctx.getBean("flink-1.1-FLINK-3477", classOf[Flink]),
    command /**/ =
      s"""
         |-v -c de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink.DatasetAGenerator  \\
         |$${app.path.datagens}/flink-hashagg-datagens-1.0-SNAPSHOT.jar                      \\
         |$${system.default.config.parallelism.total}                                        \\
         |$tuplesPerTask                                                                     \\
         |$keyCardinality                                                                    \\
         |$keyDist                                                                           \\
         |$${system.hadoop-2.path.input}/$name.${keyDist.split('[')(0).toLowerCase}
      """.stripMargin.trim
  )

  // ---------------------------------------------------
  // Data Sets
  // ---------------------------------------------------

  def ds(name: String, tuplesPerTask: Long, keyCardinality: Int, keyDist: String): DataSet = new GeneratedDataSet(
    src /**/ = dg(name, tuplesPerTask, keyCardinality, keyDist),
    dst /**/ = s"$${system.hadoop-2.path.input}/$name.$keyDist",
    fs /* */ = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  // ds-A1

  @Bean(name = Array("ds-A1.uniform"))
  def `ds-A1.uniform`: DataSet =
    ds("ds-A1", 40000000L, 40000, "Uniform")

  @Bean(name = Array("ds-A1.binomial"))
  def `ds-A1.binomial`: DataSet =
    ds("ds-A1", 40000000L, 40000, "Binomial[0.5]")

  @Bean(name = Array("ds-A1.zipf"))
  def `ds-A1.zipf`: DataSet =
    ds("ds-A1", 40000000L, 40000, "Zipf[1]")

  // ds-A2

  @Bean(name = Array("ds-A2.uniform"))
  def `ds-A2.uniform`: DataSet =
    ds("ds-A2", 40000000L, 400000, "Uniform")

  @Bean(name = Array("ds-A2.binomial"))
  def `ds-A2.binomial`: DataSet =
    ds("ds-A2", 40000000L, 400000, "Binomial[0.5]")

  @Bean(name = Array("ds-A2.zipf"))
  def `ds-A2.zipf`: DataSet =
    ds("ds-A2", 40000000L, 400000, "Zipf[1]")

  // ds-A3

  @Bean(name = Array("ds-A3.uniform"))
  def `ds-A3.uniform`: DataSet =
    ds("ds-A3", 40000000L, 4000000, "Uniform")

  @Bean(name = Array("ds-A3.binomial"))
  def `ds-A3.binomial`: DataSet =
    ds("ds-A3", 40000000L, 4000000, "Binomial[0.5]")

  @Bean(name = Array("ds-A3.zipf"))
  def `ds-A3.zipf`: DataSet =
    ds("ds-A3", 40000000L, 4000000, "Zipf[1]")

  // ---------------------------------------------------
  // Output Paths
  // ---------------------------------------------------

  @Bean(name = Array("wl-X.output"))
  def `wl-X.output`: ExperimentOutput = new ExperimentOutput(
    path /**/ = "${system.hadoop-2.path.output}/wl-X",
    fs /*  */ = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  @Bean(name = Array("wl-Y.output"))
  def `wl-Y.output`: ExperimentOutput = new ExperimentOutput(
    path /**/ = "${system.hadoop-2.path.output}/wl-Y",
    fs /*  */ = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )
}