package com.api.services.api

import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.model.headers.HttpCookie
import com.api.models.UserEntry
import com.api.server._

import scala.concurrent.ExecutionContext
import akka.stream.Materializer
import com.api.ApiConfig
import com.api.services.api.UserService.UserState
import com.api.services.security.ApiSecurity

object UserService {
  case class UserState(
    db: Map[String, UserEntry] = Map.empty[String, UserEntry],
    active: Map[String, String] = Map.empty[String, String]
  ){
    def findByToken(token: String): Option[UserEntry] = active.get(token).flatMap(db.get)
    def findByEmail(email: String): Option[UserEntry] = db.get(email)
    def updateDb(userEntry: UserEntry): UserState = copy(db = db + (userEntry.email -> userEntry))
    def updateActive(userEntry: UserEntry, token: String): UserState = copy(active = active + (token -> userEntry.email))
  }

  case class ApiResult(response: ApiHttpResponse, state: UserState)
}

class UserService(config: ApiConfig)(implicit system: ActorSystem, ec: ExecutionContext, m: Materializer)
  extends Actor {

  override def receive: Receive = active(UserState())

  def active(userState: UserState): Receive = {
    case request: RegisterRequest =>
      userState.findByEmail(request.email) match {
        case Some(_) =>
          sender() ! ApiHttpResponse(400, Some(ErrorResponse("Found user with same email")))
        case None =>
          context.become(active(userState.updateDb(request.toUserEntry)))
          sender() ! ApiHttpResponse(200)
      }
    case request: FormRequest =>
      userState.findByEmail(request.email) match {
        case Some(found) if ApiSecurity.checkPassword(found, request.password) =>
          val token = ApiSecurity.cookie
          context.become(active(userState.updateActive(found, token)))
          sender() ! ApiHttpResponse(200, None, List(HttpCookie(config.tokenName, token)))
        case _ => sender() ! ApiHttpResponse(401)
      }
    case request: SelfRequest =>
      userState.findByToken(request.token) match {
        case Some(user) => sender() ! ApiHttpResponse(200, Some(SelfResponse(user.email, user.name)))
        case None => sender() ! ApiHttpResponse(403)
      }
  }
}