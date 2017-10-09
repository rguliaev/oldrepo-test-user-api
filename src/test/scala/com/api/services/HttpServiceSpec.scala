package com.api.services

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, Multipart, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestKit
import com.api.services.http.HttpService
import com.api.{ApiConfig, ApiConfigLoader, Common}
import com.api.services.api.UserService
import org.scalatest.{Matchers, WordSpec}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import akka.http.scaladsl.server.MissingFormFieldRejection
import akka.http.scaladsl.unmarshalling.Unmarshaller._

import scala.concurrent.duration._

class HttpServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with ApiConfigLoader with Common {

  val config: ApiConfig = loadConfig.getOrElse(fail("ApiConfig hasn't loaded"))
  val userService: ActorRef = system.actorOf(Props(new UserService(config)))
  val service = new HttpService(userService, config)

  override def afterAll(): Unit = {
    Http.get(system).shutdownAllConnectionPools().onComplete(_ => TestKit.shutdownActorSystem(system))
  }

  implicit val timeout = RouteTestTimeout(5.seconds)

  private var token = ""

  "The service" should {
    "pass register request with right data" in {
      Post("/user", registerRequest1) ~> service.route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "pass auth request with right data" in {
      val email = Multipart.FormData.BodyPart.Strict("email", registerRequest1.email)
      val password = Multipart.FormData.BodyPart.Strict("password", registerRequest1.password)
      val formData = Multipart.FormData(email, password)
      Post(s"/auth/form", formData) ~> service.route ~> check {
        header[`Set-Cookie`].exists(_.cookie.name == config.tokenName) should be (true)
        token = header[`Set-Cookie`].map(_.cookie.value).getOrElse(fail("No token found"))
        status shouldEqual StatusCodes.OK
      }
    }

    "not pass auth request with wrong data" in {
      val email = Multipart.FormData.BodyPart.Strict("email1", registerRequest1.email)
      val password = Multipart.FormData.BodyPart.Strict("password1", registerRequest1.password)
      val formData = Multipart.FormData(email, password)
      Post(s"/auth/form", formData) ~> service.route ~> check {
        rejection shouldEqual MissingFormFieldRejection("email")
      }
    }

    "pass self request with right cookie data" in {
      Get("/user/self", selfRequest(token)) ~> Cookie(config.tokenName, token) ~> service.route ~> check {
        status shouldEqual StatusCodes.OK
        contentType should be (ContentTypes.`application/json`)
        entityAs[String] should be (s"""{"email":"${registerRequest1.email}","name":"${registerRequest1.name}"}""")
      }
    }

    "not pass self request with wrong cookie data" in {
      Get("/user/self", selfRequest(token)) ~> Cookie(config.tokenName, "blabla") ~> service.route ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "pass self request with right authorization header" in {
      Get("/user/self", selfRequest(token)) ~> addHeader("Authorization", token) ~> service.route ~> check {
        status shouldEqual StatusCodes.OK
        contentType should be (ContentTypes.`application/json`)
        entityAs[String] should be (s"""{"email":"${registerRequest1.email}","name":"${registerRequest1.name}"}""")
      }
    }

    "not pass self request with right authorization header" in {
      Get("/user/self", selfRequest(token)) ~> addHeader("Authorization", "blabla") ~> service.route ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}