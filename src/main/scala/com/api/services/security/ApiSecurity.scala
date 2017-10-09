package com.api.services.security

import com.api.Logging
import com.api.models.UserEntry
import com.github.t3hnar.bcrypt._

import scala.util.Random

object ApiSecurity extends Logging {
  def checkPassword(userEntry: UserEntry, password: String): Boolean =
    userEntry.password == hash(password, userEntry.salt)

  def cookie = Random.alphanumeric.take(40).mkString
  def salt = generateSalt
  def hash(data: String, salt: String) = data.bcrypt(salt)
}
