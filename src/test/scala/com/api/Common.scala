package com.api

import com.api.server.{FormRequest, RegisterRequest, SelfRequest}

trait Common {
  val registerRequest1 = RegisterRequest("1@1.com", "John", "xyz")
  val registerRequest2 = RegisterRequest("2@2.com", "Bill", "zyx")
  val wrongFormRequest = FormRequest("1@1.com", "111")
  val formRequest1 = FormRequest(registerRequest1.email, registerRequest1.password)
  def selfRequest(token: String) = SelfRequest(token)
}
