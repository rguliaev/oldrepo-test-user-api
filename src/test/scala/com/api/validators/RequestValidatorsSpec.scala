package com.api.validators

import com.api.Common
import org.scalatest.{FlatSpec, Matchers}

class RequestValidatorsSpec extends FlatSpec with Matchers with Common {
  it should "validate right Incoming Requests" in {
    assert(registerRequest1.validate)
    assert(formRequest1.validate)
    assert(selfRequest("bla bla").validate)
  }

  it should "not validate wrong Incoming Requests" in {
    assert(!registerRequest1.copy(email = "  ").validate)
    assert(!registerRequest1.copy(name = " ").validate)
    assert(!registerRequest1.copy(password = " ").validate)
    assert(!formRequest1.copy(email = "").validate)
    assert(!formRequest1.copy(password = "").validate)
    assert(!selfRequest(" ").validate)
  }
}
