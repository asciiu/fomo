package models

import java.time.OffsetDateTime
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import com.softwaremill.bootzooka.common.Utils
import com.softwaremill.bootzooka.user._
import org.joda.time.DateTime

case class User(
                 id: UUID,
                 email: String,
                 firstName: String,
                 lastName: String,
                 passwordHash: String,
                 salt: String
               )

object User {

  def withRandomUUID(email: String,
                     firstName: String,
                     lastName: String,
                     password: String,
                     salt: String) =
    User(UUID.randomUUID(), email, firstName, lastName,
      encryptPassword(password, salt), salt)

  def encryptPassword(password: String, salt: String): String = {
    // 10k iterations takes about 10ms to encrypt a password on a 2013 MacBook
    val keySpec          = new PBEKeySpec(password.toCharArray, salt.getBytes, 10000, 128)
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val bytes            = secretKeyFactory.generateSecret(keySpec).getEncoded
    Utils.toHex(bytes)
  }

  def passwordsMatch(password: String, user: User): Boolean =
    Utils.constantTimeEquals(
      user.passwordHash,
      encryptPassword(password, user.salt)
    )
}

case class BasicUserData(id: UserId, first: String, last: String, email: String)

object BasicUserData {
  def fromUser(user: User) = new BasicUserData(user.id, user.firstName, user.lastName, user.email)
}
