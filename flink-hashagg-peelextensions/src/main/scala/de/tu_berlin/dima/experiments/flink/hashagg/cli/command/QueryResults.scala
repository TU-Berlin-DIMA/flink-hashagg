package de.tu_berlin.dima.experiments.flink.hashagg.cli.command

import java.io.PrintWriter
import java.lang.{System => Sys}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

import anorm.SqlParser._
import anorm.{~, _}
import net.sourceforge.argparse4j.inf.{Namespace, Subparser}
import org.peelframework.core.cli.command.Command
import org.peelframework.core.config.loadConfig
import org.peelframework.core.results.DB
import org.peelframework.core.util.console._
import org.peelframework.core.util.shell
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import resource._

/** Query the database for the runtimes of a particular experiment. */
@Service("db:results")
class QueryResults extends Command {

  import scala.language.postfixOps

  override val help = "query the database for the runtimes of a particular experiment."

  override def register(parser: Subparser) = {
    // options
    parser.addArgument("--connection")
      .`type`(classOf[String])
      .dest("app.db.connection")
      .metavar("ID")
      .help("database config name (default: h2)")
    // arguments
    parser.addArgument("suite")
      .`type`(classOf[String])
      .dest("app.suite.name")
      .metavar("SUITE")
      .help("experiments suite to run")
    // parameters
    parser.addArgument("--output-type", "-o")
      .`type`(classOf[String])
      .choices("table", "plots")
      .dest("app.output.mode")
      .metavar("MODE")
      .help("output mode")

    // option defaults
    parser.setDefault("app.db.connection", "h2")
    parser.setDefault("app.output.mode", "plots")
  }

  override def configure(ns: Namespace) = {
    // set ns options and arguments to system properties
    Sys.setProperty("app.db.connection", ns.getString("app.db.connection"))
    Sys.setProperty("app.suite.name", ns.getString("app.suite.name"))
    Sys.setProperty("app.output.mode", ns.getString("app.output.mode"))
  }

  override def run(context: ApplicationContext) = {
    logger.info(s"Querying runtime results for suite '${Sys.getProperty("app.suite.name")}' from '${Sys.getProperty("app.db.connection")}'")

    // load application configuration
    implicit val config = loadConfig()

    // create database connection
    val connName = Sys.getProperty("app.db.connection")
    implicit val conn = DB.getConnection(connName)

    val suite = config.getString("app.suite.name")
    val outputMode = Symbol(config.getString("app.output.mode"))

    try {
      // Create an SQL query
      val result = SQL(
        """
          |SELECT   ex.name                    as name                    ,
          |         MEDIAN(er.time)            as med_time                ,
          |         AVG(d1.v_long)             as avg_num_emissions       ,
          |         AVG(d2.v_long)             as avg_num_emitted_records
          |FROM     experiment       as ex                                ,
          |         experiment_run   as er                                ,
          |         experiment_event as d1                                ,
          |         experiment_event as d2
          |WHERE    ex.id    = er.experiment_id
          |AND      er.id    = d1.experiment_run_id
          |AND      er.id    = d2.experiment_run_id
          |AND      d1.name  = 'numEmissions'
          |AND      d2.name  = 'numEmittedRecords'
          |AND      ex.suite = {suite}
          |GROUP BY name
          |ORDER BY name;
        """.stripMargin.trim
      )
        .on("suite" -> suite)
        .as({
          get[String]("name") ~ get[Int]("med_time") ~ get[Double]("avg_num_emissions") ~ get[Double]("avg_num_emitted_records") map {
            case name ~ medTime ~ avgNumEmissions ~ avgNumEmittedRecords => (name, medTime, avgNumEmissions, avgNumEmittedRecords)
          }
        } *)

      outputMode match {
        case 'table => // render results as txt

          System.out.println(Seq(
            "name" /*                         */ padTo(25, ' '),
            "med_time" /*                     */ padTo(10, ' '),
            "avg_num_emissions" /*            */ padTo(20, ' '),
            "avg_num_emitted_records" /*      */ padTo(25, ' ')
          ) mkString("|", "|", "|"))

          System.out.println(Seq(25, 10, 20, 25) map (l => "-" * l) mkString("|", "|", "|"))

          for ((name, medTime, avgNumEmissions, avgNumEmittedRecords) <- result) {
            System.out.println(Seq(
              s"$name" /*                     */ padTo(25, ' '),
              s"$medTime" /*                  */ padTo(10, ' '),
              f"$avgNumEmissions%1.2f" /*     */ padTo(20, ' '),
              f"$avgNumEmittedRecords%1.2f" /**/ padTo(25, ' ')
            ) mkString("|", "|", "|"))
          }

        case 'plots => // render results as png
          val gplsPath = Paths.get(config.getString("app.path.utils"), "gpl")
          val basePath = Paths.get(config.getString("app.path.results"), suite, "plots")
          val dataPath = basePath.resolve("results.dat")

          // ensure base folder exists
          shell.ensureFolderIsWritable(basePath)

          // write data file
          logger.info(s"Writing query results into path '$dataPath'")

          for {
            bwriter <- managed(Files.newBufferedWriter(dataPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE))
            pwriter <- managed(new PrintWriter(bwriter))
          } {
            for (((name, medTime, avgNumEmissions, avgNumEmittedRecords), i) <- result zipWithIndex) {
              val offset = i * 0.5 + 0.5 * (i / 2)
              pwriter.println(Seq(
                f"$offset%1.2f" /*              */ padTo(10, ' '),
                s"$name" /*                     */ padTo(25, ' '),
                s"$medTime" /*                  */ padTo(10, ' '),
                f"$avgNumEmissions%1.2f" /*     */ padTo(20, ' '),
                f"$avgNumEmittedRecords%1.2f" /**/ padTo(25, ' ')
              ) mkString "")
            }
          }

          // execute gnuplot scripts
          logger.info(s"Plotting query results using Gnuplot")

          // plot runtimes
          shell !(
            cmd /**/ = s"""gnuplot -e "gplsPath='$gplsPath'" -e "basePath='$basePath'" -e "suite='$suite'" $gplsPath/runtimes.gpl""",
            errorMsg = "Cannot plot 'runtimes.gpl' with Gnuplot")
          // plot # emissions
          shell !(
            cmd /**/ = s"""gnuplot -e "gplsPath='$gplsPath'" -e "basePath='$basePath'" -e "suite='$suite'" $gplsPath/emissions.gpl""",
            errorMsg = "Cannot plot 'runtimes.gpl' with Gnuplot")
          // plot # emitted records
          shell !(
            cmd /**/ = s"""gnuplot -e "gplsPath='$gplsPath'" -e "basePath='$basePath'" -e "suite='$suite'" $gplsPath/records.gpl""",
            errorMsg = "Cannot plot 'runtimes.gpl' with Gnuplot")
      }


    }
    catch {
      case e: Throwable =>
        logger.error(s"Error while querying runtime results for suite '$suite'".red)
        throw e
    } finally {
      logger.info(s"Closing connection to database '$connName'")
      conn.close()
      logger.info("#" * 60)
    }
  }
}
