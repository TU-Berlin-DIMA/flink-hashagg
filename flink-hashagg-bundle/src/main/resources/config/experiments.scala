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

  val runs = 5

  /* The enclosing application context. */
  var ctx: ApplicationContext = null

  def setApplicationContext(ctx: ApplicationContext): Unit = {
    this.ctx = ctx
  }

  // ---------------------------------------------------
  // Experiments
  // ---------------------------------------------------

  def ex(name: String, ds: String, wl: String): ExperimentSuite = new ExperimentSuite(
    for {
      distribution /**/ <- Seq("uniform", "binomial", "zipf")
      strategy /*    */ <- Seq("hash", "sort")
    } yield new FlinkExperiment(
      name /*   */ = s"$ds.$distribution.$wl.$strategy",
      command /**/ =
        s"""
           |-v -c de.tu_berlin.dima.experiments.flink.hashagg.flink.Workload${wl.split('-')(1)} \\
           |$${app.path.apps}/flink-hashagg-flink-jobs-1.0.0.jar                                \\
           |$strategy                                                                           \\
           |$${system.hadoop-2.path.input}/$ds.$distribution                                    \\
           |$${system.hadoop-2.path.output}/$wl
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
      inputs /* */ = Set(ctx.getBean(s"$ds.$distribution", classOf[DataSet])),
      outputs /**/ = Set(ctx.getBean(s"$wl.output", classOf[ExperimentOutput]))
    )
  )

  // { A1, A2, A3 } × { X, Y }

  @Bean(name = Array("ex-A1.X"))
  def `ex-A1.X`: ExperimentSuite =
    ex(name = "ex-A1.X", "ds-A1", "wl-X")

  @Bean(name = Array("ex-A1.Y"))
  def `ex-A1.Y`: ExperimentSuite =
    ex(name = "ex-A1.Y", "ds-A1", "wl-Y")

  @Bean(name = Array("ex-A2.X"))
  def `ex-A2.X`: ExperimentSuite =
    ex(name = "ex-A2.X", "ds-A2", "wl-X")

  @Bean(name = Array("ex-A2.Y"))
  def `ex-A2.Y`: ExperimentSuite =
    ex(name = "ex-A2.Y", "ds-A2", "wl-Y")

  @Bean(name = Array("ex-A3.X"))
  def `ex-A3.X`: ExperimentSuite =
    ex(name = "ex-A3.X", "ds-A3", "wl-X")

  @Bean(name = Array("ex-A3.Y"))
  def `ex-A3.Y`: ExperimentSuite =
    ex(name = "ex-A3.Y", "ds-A3", "wl-Y")
}
