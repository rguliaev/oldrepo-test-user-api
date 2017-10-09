package com.api.server

import akka.http.scaladsl.model.headers.HttpCookie
import com.api.models.UserEntry
import com.api.services.security.ApiSecurity
import io.circe._, io.circe.syntax._, io.circe.generic.auto._

trait ApiRequest {
  def validate: Boolean
}
case class RegisterRequest(email: String, name: String, password: String) extends ApiRequest {
  def toUserEntry: UserEntry = {
    val salt = ApiSecurity.salt
    val encryptedPassword = ApiSecurity.hash(password, salt)
    UserEntry(email, name, encryptedPassword, salt)
  }

  override def validate: Boolean = email.trim.nonEmpty && name.trim.nonEmpty && password.trim.nonEmpty
}
case class FormRequest(email: String, password: String) extends ApiRequest {
  override def validate: Boolean = email.trim.nonEmpty && password.trim.nonEmpty
}
case class SelfRequest(token: String) extends ApiRequest {
  override def validate: Boolean = token.trim.nonEmpty
}

case class ApiHttpResponse(
  code: Int,
  body: Option[ApiResponse] = None,
  cookies: List[HttpCookie] = Nil)

trait ApiResponse
case class SelfResponse(email: String, name: String) extends ApiResponse
case class ErrorResponse(msg: String) extends ApiResponse

object ApiResponse {
  implicit val encoder: Encoder[ApiResponse] = Encoder.instance {
    case selfResponse @ SelfResponse(_, _) => selfResponse.asJson
    case errorResponse @ ErrorResponse(_) => errorResponse.asJson
  }
}