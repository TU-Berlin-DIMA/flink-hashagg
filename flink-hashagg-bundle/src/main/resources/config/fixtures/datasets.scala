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

  def `dagagen.A`(distribution: String): FlinkJob = new FlinkJob(
    runner  = ctx.getBean("flink-1.1-FLINK-3477", classOf[Flink]),
    command =
      s"""
        |-v -c de.tu_berlin.dima.experiments.flink.hashagg.datagen.flink.DatasetAGenerator  \\
        |$${app.path.datagens}/flink-hashagg-datagens-1.0-SNAPSHOT.jar                      \\
        |$${system.default.config.parallelism.total}                                        \\
        |1000000                                                                            \\
        |100000                                                                             \\
        |$distribution                                                                      \\
        |$${system.hadoop-2.path.input}/dataset-A.${distribution.split('[')(0).toLowerCase}
      """.stripMargin.trim
  )

  @Bean(name = Array("datagen.A.uniform"))
  def `datagen.A.uniform`: FlinkJob =
    `dagagen.A`("Uniform")

  @Bean(name = Array("datagen.A.binomial"))
  def `datagen.A.binomial`: FlinkJob =
    `dagagen.A`("Binomial[0.5]")

  @Bean(name = Array("datagen.A.zipf"))
  def `datagen.A.zipf`: FlinkJob =
    `dagagen.A`("Zipf[1]")

  // ---------------------------------------------------
  // Data Sets
  // ---------------------------------------------------

  def `dataset.A`(distribution: String): DataSet = new GeneratedDataSet(
    src = ctx.getBean(s"datagen.A.$distribution", classOf[FlinkJob]),
    dst = s"$${system.hadoop-2.path.input}/dataset-A.$distribution",
    fs  = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  @Bean(name = Array("dataset.A.uniform"))
  def `dataset.A.uniform`: DataSet =
    `dataset.A`("uniform")

  @Bean(name = Array("dataset.A.binomial"))
  def `dataset.A.binomial`: DataSet =
    `dataset.A`("binomial")

  @Bean(name = Array("dataset.A.zipf"))
  def `dataset.A.zipf`: DataSet =
    `dataset.A`("zipf")

  @Bean(name = Array("workload-A.output"))
  def `workload-A.output`: ExperimentOutput = new ExperimentOutput(
    path = "${system.hadoop-2.path.output}/workload-A",
    fs  = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  @Bean(name = Array("workload-B.output"))
  def `workload-B.output`: ExperimentOutput = new ExperimentOutput(
    path = "${system.hadoop-2.path.output}/workload-B",
    fs  = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )
}