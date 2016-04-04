/**
 * Copyright (C) ${project.inceptionYear} TU Berlin (alexander.alexandrov@tu-berlin.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tu_berlin.dima.experiments.flink.hashagg.cli.command

import java.lang.{System => Sys}

import anorm.SqlParser._
import anorm._
import org.peelframework.core.cli.command.Command
import org.peelframework.core.config.loadConfig
import org.peelframework.core.results.DB
import org.peelframework.core.util.console._
import net.sourceforge.argparse4j.inf.{Namespace, Subparser}
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/** Query the database for the runtimes of a particular experiment. */
@Service("query:runtimes")
class QueryRuntimes extends Command {

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
    // arguments
    parser.addArgument("--aggregation-mode", "-a")
      .`type`(classOf[String])
      .choices("min", "max", "mean", "median")
      .dest("app.aggregation.mode")
      .metavar("MODE")
      .help("aggregation mode")

    // option defaults
    parser.setDefault("app.db.connection", "h2")
    parser.setDefault("app.aggregation.mode", "median")
  }

  override def configure(ns: Namespace) = {
    // set ns options and arguments to system properties
    Sys.setProperty("app.db.connection", ns.getString("app.db.connection"))
    Sys.setProperty("app.suite.name", ns.getString("app.suite.name"))
    Sys.setProperty("app.aggregation.mode", ns.getString("app.aggregation.mode"))
  }

  override def run(context: ApplicationContext) = {
    logger.info(s"Querying runtime results for suite '${Sys.getProperty("app.suite.name")}' from '${Sys.getProperty("app.db.connection")}'")

    // load application configuration
    implicit val config = loadConfig()

    // create database connection
    val connName = Sys.getProperty("app.db.connection")
    implicit val conn = DB.getConnection(connName)

    val suite = config.getString("app.suite.name")
    val mode = Symbol(config.getString("app.aggregation.mode"))

    try {
      // Create an SQL query
      val runtimes = SQL(
        """
          |SELECT   e.name         as name  ,
          |         r.id           as rid   ,
          |         r.run          as run   ,
          |         r.time         as time
          |FROM     experiment     as e     ,
          |         experiment_run as r
          |WHERE    e.id    = r.experiment_id
          |AND      e.suite = {suite}
          |ORDER BY name, run
        """.stripMargin.trim
      )
        .on("suite" -> suite)
        .as({
          get[String]("name") ~ get[Int]("rid") ~ get[Int]("run") ~ get[Int]("time") map {
            case name ~ rid ~ run ~ time => (name, rid, run, time)
          }
        } *)
        .groupBy { case (name, _, _, _) => name }.toSeq
        .map { case (name, runs) => (name, runs.sortBy { case (_, _, _, time) => time }) }
        .sortBy { case (name, _) => name }

      // render results table

      val colLength = 35
      val cols = List("name") ++ aggColNames(mode)

      System.out.println("-" * colLength * cols.size)
      System.out.println(cols.map(_.padTo(colLength, ' ')).mkString(""))
      System.out.println("-" * colLength * cols.size)

      for ((name, runs) <- runtimes) {
        val row = List(name) ++ aggData(mode, runs)
        System.out.println(row.map(_.toString.padTo(colLength, ' ')).mkString(""))
      }

      System.out.println("-" * colLength * cols.size)
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

  def aggColNames(mode: Symbol): List[String] = mode match {
    case 'min =>
      List("min time (ms)", "run", "run id")
    case 'max =>
      List("max time (ms)", "run", "run id")
    case 'mean =>
      List("mean time (ms)", "std dev time (ms)")
    case 'median =>
      List("median time (ms)", "run", "run id")
  }

  def aggData(mode: Symbol, runs: List[(String, Int, Int, Int)]): List[Any] = mode match {
    case 'min =>
      val data = runs.map { case (_, rid, run, time) => (time, run, rid) }.minBy { case (time, run, rid) => time }
      List(data._1, data._2, data._3)
    case 'max =>
      val data = runs.map { case (_, rid, run, time) => (time, run, rid) }.maxBy { case (time, run, rid) => time }
      List(data._1, data._2, data._3)
    case 'mean =>
      val X = runs.map { case (_, rid, run, time) => time }
      val μ = X.sum / X.size.toDouble
      val σ = Math.sqrt(X.map { time => (time - μ) * (time - μ) }.sum / X.size.toDouble)
      List(f"$μ%1.2f", f"$σ%1.2f")
    case 'median =>
      val data = runs.map { case (_, rid, run, time) => (time, run, rid) }.apply((runs.size + 1) / 2)
      List(data._1, data._2, data._3)
  }
}
