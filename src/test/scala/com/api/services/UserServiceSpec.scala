package com.api.services

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit}
import com.api.server._
import com.api.{Common, ApiConfig, ApiConfigLoader}
import com.api.services.api.UserService
import org.scalatest._
import scala.concurrent.ExecutionContext

class UserServiceSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with ApiConfigLoader with Common {

  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val config: ApiConfig = loadConfig.getOrElse(fail("ApiConfig hasn't loaded"))
  val userService: ActorRef = system.actorOf(Props(new UserService(config)))

  override def afterAll(): Unit =
    Http().shutdownAllConnectionPools().onComplete(_ => TestKit.shutdownActorSystem(system))

  private var token = ""

  "An UserService actor" must {

    "register a user with unique email" in {
      userService ! registerRequest1
      expectMsg(ApiHttpResponse(200))
    }

    "not register a user with same email two times" in {
      userService ! registerRequest1
      expectMsgPF() {
        case msg: ApiHttpResponse =>
          msg.code should be (400)
          msg.body should be ('defined)
          msg.body.get shouldBe a [ErrorResponse]
          msg.body.get.asInstanceOf[ErrorResponse].msg should be ("Found user with same email")
          msg.cookies should have size 0
      }
    }

    "register another user with unique email" in {
      userService ! registerRequest2
      expectMsg(ApiHttpResponse(200))
    }

    "not pass auth form with wrong credentials" in {
      userService ! wrongFormRequest
      expectMsg(ApiHttpResponse(401))
    }

    "pass auth form with right credentials and define token" in {
      userService ! formRequest1
      expectMsgPF() {
        case msg: ApiHttpResponse =>
          msg.code should be (200)
          msg.body should be (None)
          msg.cookies should have length 1
          msg.cookies.head.value should have length 40
          token = msg.cookies.head.value
      }
    }

    "not return self response with wrong token" in {
      userService ! selfRequest("bla bla")
      expectMsg(ApiHttpResponse(403))
    }

    "return self response with right token" in {
      userService ! selfRequest(token)
      expectMsgPF() {
        case msg: ApiHttpResponse =>
          msg.code should be (200)
          msg.body should be ('defined)
          msg.body.get shouldBe a [SelfResponse]
          msg.body.get.asInstanceOf[SelfResponse].email should be (registerRequest1.email)
          msg.body.get.asInstanceOf[SelfResponse].name should be (registerRequest1.name)
      }
    }
  }
}