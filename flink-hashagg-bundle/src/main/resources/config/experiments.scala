package config

import com.typesafe.config.ConfigFactory
import org.peelframework.core.beans.data.{DataSet, ExperimentOutput}
import org.peelframework.core.beans.experiment.ExperimentSuite
import org.peelframework.flink.beans.experiment.FlinkExperiment
import org.peelframework.flink.beans.system.Flink
import org.springframework.context.annotation._
import org.springframework.context.{ApplicationContext, ApplicationContextAware}

/** Experiments definitions for the 'flink-hashagg' bundle. */
@Configuration
@ComponentScan( // Scan for annotated Peel components in the 'de.tu_berlin.dima.experiments.flink.hashagg' package
  value = Array("de.tu_berlin.dima.experiments.flink.hashagg"),
  useDefaultFilters = false,
  includeFilters = Array[ComponentScan.Filter](
    new ComponentScan.Filter(value = Array(classOf[org.springframework.stereotype.Service])),
    new ComponentScan.Filter(value = Array(classOf[org.springframework.stereotype.Component]))
  )
)
@ImportResource(value = Array(
  "classpath:peel-core.xml",
  "classpath:peel-extensions.xml"
))
@Import(value = Array(
  classOf[org.peelframework.extensions], // base system beans
  classOf[config.fixtures.systems],      // custom system beans
  classOf[config.fixtures.datasets]      // dataset beans
))
class experiments extends ApplicationContextAware {

  /* The enclosing application context. */
  var ctx: ApplicationContext = null

  def setApplicationContext(ctx: ApplicationContext): Unit = {
    this.ctx = ctx
  }

  // ---------------------------------------------------
  // Experiments
  // ---------------------------------------------------

  @Bean(name = Array("experiment.A"))
  def `wordcount.default`: ExperimentSuite = new ExperimentSuite(
    for {
      strategy     <- Seq("hash", "sort")
      distribution <- Seq("uniform", "binomial", "zipf")
    } yield new FlinkExperiment(
      name    = s"experiment.A.$strategy.$distribution",
      command =
        s"""
           |-v -c de.tu_berlin.dima.experiments.flink.hashagg.flink.WorkloadA \\
           |$${app.path.apps}/flink-hashagg-flink-jobs-1.0-SNAPSHOT.jar       \\
           |$strategy                                                         \\
           |$${system.hadoop-2.path.input}/dataset-A                          \\
           |$${system.hadoop-2.path.output}/workload-A
        """.stripMargin.trim,
      config  = ConfigFactory.parseString(""),
      runs    = 3,
      runner  = ctx.getBean("flink-1.1-FLINK-3477", classOf[Flink]),
      inputs  = Set(ctx.getBean(s"dataset.A.$distribution", classOf[DataSet])),
      outputs = Set(ctx.getBean(s"workload-A.output", classOf[ExperimentOutput]))
    )
  )
}
