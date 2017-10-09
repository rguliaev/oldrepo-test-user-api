package com.api

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.api.services.http.HttpService
import com.api.services.api.UserService
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object Application extends App with ApiConfigLoader with Logging {

  implicit val system: ActorSystem = ActorSystem("user-api")
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  loadConfig match {
    case Success(config) =>
      val userService: ActorRef = system.actorOf(Props(new UserService(config)))
      val router = new HttpService(userService, config)
      val bindingFuture = Http().bindAndHandle(router.route, config.server.host, config.server.port)
      log.info("User Api service has been started ...")
      sys.addShutdownHook {
        log.info("Shutting down...")
        val theEnd = for {
          bind <- bindingFuture
          _ <- bind.unbind()
          _ <- Http().shutdownAllConnectionPools()
        } yield ()

        theEnd.onComplete(_ => system.terminate())
      }
    case Failure(ex) =>
      log.error(ex.getMessage)
      system.terminate()
  }
}

case class ApiConfig(server: ServerConfig, tokenName: String)
case class ServerConfig(host: String, port: Int)

trait ApiConfigLoader {
  def loadConfig: Try[ApiConfig] = Try {
    val conf = ConfigFactory.load()
    val server = ServerConfig(conf.getString("server.host"), conf.getInt("server.port"))
    val tokenName = conf.getString("token.name")
    ApiConfig(server, tokenName)
  }
}

trait Logging {
  val log: Logger = LoggerFactory.getLogger(getClass)
}