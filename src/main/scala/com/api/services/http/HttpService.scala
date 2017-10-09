package com.api.services.http

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.api.server._
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.api.{ApiConfig, Logging}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class HttpService(userService: ActorRef, config: ApiConfig)(implicit executionContext: ExecutionContext) extends Logging {

  implicit val timeout: Timeout = Timeout(5.seconds)

  private def processRequest[T <: ApiRequest](request: T): Route =
    if (request.validate) run(request) else complete((400, "Invalid validation"))

  private def run[T <: ApiRequest](data: T) = {
    val future = (userService ? data).mapTo[ApiHttpResponse] recover { case ex =>
      ApiHttpResponse(400, Some(ErrorResponse("Something went wrong")))
    }
    onSuccess(future) { response: ApiHttpResponse =>
      if (response.cookies.nonEmpty) {
        setCookie(response.cookies.head, response.cookies.tail:_*) {
          complete((response.code, response.body))
        }
      } else complete((response.code, response.body))
    }
  }

  val route: Route =
    path("user") {
      post {
        entity(as[RegisterRequest]) { request =>
          processRequest(request)
        }
      }
    } ~
      path("user" / "self") {
        get {
          optionalCookie(config.tokenName) {
            case Some(key) => processRequest(SelfRequest(key.value))
            case None =>
              optionalHeaderValueByName("Authorization") {
                case Some(key) => processRequest(SelfRequest(key))
                case None => complete(StatusCodes.Forbidden)
              }
          }
        }
      } ~
        path("auth" / "form") {
          post {
            formFields('email.as[String], 'password.as[String]).as(FormRequest) { form =>
              processRequest(form)
            }
          }
        }
}