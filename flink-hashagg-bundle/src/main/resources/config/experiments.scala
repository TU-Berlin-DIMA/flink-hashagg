package config

import com.typesafe.config.ConfigFactory
import org.peelframework.core.beans.data.{DataSet, ExperimentOutput}
import org.peelframework.core.beans.experiment.ExperimentSuite
import org.peelframework.dstat.beans.system.Dstat
import org.peelframework.flink.beans.experiment.FlinkExperiment
import org.peelframework.flink.beans.system.Flink
import org.springframework.context.annotation._
import org.springframework.context.{ApplicationContext, ApplicationContextAware}

/** Experiments definitions for the 'flink-hashagg' bundle. */
@Configuration
@ComponentScan(// Scan for annotated Peel components in the 'de.tu_berlin.dima.experiments.flink.hashagg' package
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
  classOf[config.fixtures.systems], // custom system beans
  classOf[config.fixtures.datasets] // dataset beans
))
class experiments extends ApplicationContextAware {

  val runs = 7

  /* The enclosing application context. */
  var ctx: ApplicationContext = null

  def setApplicationContext(ctx: ApplicationContext): Unit = {
    this.ctx = ctx
  }

  // ---------------------------------------------------
  // Experiments
  // ---------------------------------------------------

  @Bean(name = Array("ex-A"))
  def `ex-A`: ExperimentSuite = new ExperimentSuite(
    for {
      distribution /**/ <- Seq("uniform", "binomial", "zipf")
      strategy /*    */ <- Seq("hash", "sort")
    } yield new FlinkExperiment(
      name /*   */ = s"ds-A.$distribution.wl-A.$strategy",
      command /**/ =
        s"""
           |-v -c de.tu_berlin.dima.experiments.flink.hashagg.flink.WorkloadA \\
           |$${app.path.apps}/flink-hashagg-flink-jobs-1.0-SNAPSHOT.jar       \\
           |$strategy                                                         \\
           |$${system.hadoop-2.path.input}/ds-A.$distribution                 \\
           |$${system.hadoop-2.path.output}/wl-A
        """.stripMargin.trim,
      config /* */ = ConfigFactory.parseString(
        s"""
           |system.flink.config.yaml {
           |  # 1 GiB of memory
           |  taskmanager.heap.mb = 1024
           |  # 0.5 * 1 = 0.5 GiB will be managed
           |  taskmanager.memory.fraction = 0.5
           |  # 16384 * 16384 = 0.25 GiB memory for network
           |  taskmanager.network.numberOfBuffers = 16384
           |  taskmanager.network.bufferSizeInBytes = 16384
           |}
         """.stripMargin),
      runs /*   */ = runs,
      runner /* */ = ctx.getBean("flink-1.1-FLINK-3477", classOf[Flink]),
      systems /**/ = Set(ctx.getBean("dstat-0.7.2", classOf[Dstat])),
      inputs /* */ = Set(ctx.getBean(s"ds-A.$distribution", classOf[DataSet])),
      outputs /**/ = Set(ctx.getBean(s"wl-A.output", classOf[ExperimentOutput]))
    )
  )

  @Bean(name = Array("ex-B"))
  def `ex-B`: ExperimentSuite = new ExperimentSuite(
    for {
      distribution <- Seq("uniform", "binomial", "zipf")
      strategy <- Seq("hash", "sort")
    } yield new FlinkExperiment(
      name /*   */ = s"ds-A.$distribution.wl-B.$strategy",
      command /**/ =
        s"""
           |-v -c de.tu_berlin.dima.experiments.flink.hashagg.flink.WorkloadB \\
           |$${app.path.apps}/flink-hashagg-flink-jobs-1.0-SNAPSHOT.jar       \\
           |$strategy                                                         \\
           |$${system.hadoop-2.path.input}/ds-A.$distribution                 \\
           |$${system.hadoop-2.path.output}/wl-B
        """.stripMargin.trim,
      config /* */ = ConfigFactory.parseString(
        s"""
           |system.flink.config.yaml {
           |  # 1 GiB of memory
           |  taskmanager.heap.mb = 1024
           |  # 0.5 * 1 = 0.5 GiB will be managed
           |  taskmanager.memory.fraction = 0.5
           |  # 16384 * 16384 = 0.25 GiB memory for network
           |  taskmanager.network.numberOfBuffers = 16384
           |  taskmanager.network.bufferSizeInBytes = 16384
           |}
         """.stripMargin),
      runs /*   */ = runs,
      runner /* */ = ctx.getBean("flink-1.1-FLINK-3477", classOf[Flink]),
      systems /**/ = Set(ctx.getBean("dstat-0.7.2", classOf[Dstat])),
      inputs /* */ = Set(ctx.getBean(s"ds-A.$distribution", classOf[DataSet])),
      outputs /**/ = Set(ctx.getBean(s"wl-B.output", classOf[ExperimentOutput]))
    )
  )
}
