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

  def `dg-A`(distribution: String): FlinkJob = new FlinkJob(
    runner /* */ = ctx.getBean("flink-1.1-FLINK-3477", classOf[Flink]),
    command /**/ =
      s"""
         |-v -c de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink.DatasetAGenerator  \\
         |$${app.path.datagens}/flink-hashagg-datagens-1.0-SNAPSHOT.jar                      \\
         |$${system.default.config.parallelism.total}                                        \\
         |20000000                                                                           \\
         |100000                                                                             \\
         |$distribution                                                                      \\
         |$${system.hadoop-2.path.input}/ds-A.${distribution.split('[')(0).toLowerCase}
      """.stripMargin.trim
  )

  @Bean(name = Array("dg-A.uniform"))
  def `dg-A.uniform`: FlinkJob =
    `dg-A`("Uniform")

  @Bean(name = Array("dg-A.binomial"))
  def `dg-A.binomial`: FlinkJob =
    `dg-A`("Binomial[0.5]")

  @Bean(name = Array("dg-A.zipf"))
  def `dg-A.zipf`: FlinkJob =
    `dg-A`("Zipf[1]")

  // ---------------------------------------------------
  // Data Sets
  // ---------------------------------------------------

  def `ds-A`(distribution: String): DataSet = new GeneratedDataSet(
    src /**/ = ctx.getBean(s"dg-A.$distribution", classOf[FlinkJob]),
    dst /**/ = s"$${system.hadoop-2.path.input}/ds-A.$distribution",
    fs /* */ = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  @Bean(name = Array("ds-A.uniform"))
  def `ds-A.uniform`: DataSet =
    `ds-A`("uniform")

  @Bean(name = Array("ds-A.binomial"))
  def `ds-A.binomial`: DataSet =
    `ds-A`("binomial")

  @Bean(name = Array("ds-A.zipf"))
  def `ds-A.zipf`: DataSet =
    `ds-A`("zipf")

  @Bean(name = Array("wl-A.output"))
  def `wl-A.output`: ExperimentOutput = new ExperimentOutput(
    path /**/ = "${system.hadoop-2.path.output}/wl-A",
    fs /*  */ = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  @Bean(name = Array("wl-B.output"))
  def `wl-B.output`: ExperimentOutput = new ExperimentOutput(
    path /**/ = "${system.hadoop-2.path.output}/wl-B",
    fs /*  */ = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )
}