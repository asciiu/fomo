package com.flowy.common.models

import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import com.flowy.common.utils.Utils
import com.flowy.common.models.User.UserId
import io.circe.{Encoder, Json}


case class User(
                 id: UUID,
                 email: String,
                 firstName: String,
                 lastName: String,
                 passwordHash: String,
                 salt: String
               )

object User {
  type UserId = UUID

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

case class Balance(id: UUID,
                   userId: UUID,
                   apiKeyId: UUID,
                   exchange: Exchange.Value,
                   currency: String,
                   currencyLong: String,
                   blockchainAddress: Option[String],
                   availableBalance: Double,
                   exchangeTotalBalance: Double,
                   exchangeAvailableBalance: Double,
                   pending: Option[Double])

object Balance {
  implicit val encodeBalance: Encoder[Balance] = new Encoder[Balance] {
    final def apply(balance: Balance): Json = {
      val pending = balance.pending match {
        case Some(bal) => Json.fromDoubleOrNull(bal)
        case None => Json.Null
      }

      Json.obj(
        ("currency", Json.fromString(balance.currency)),
        ("availableBalance", Json.fromDoubleOrNull(balance.availableBalance)),
        ("exchangeTotalBalance", Json.fromDoubleOrNull(balance.exchangeTotalBalance)),
        ("exchangeAvailableBalance", Json.fromDoubleOrNull(balance.exchangeAvailableBalance)),
        ("pending", pending)
      )
    }
  }
}

case class UserData(id: UserId, first: String, last: String, email: String, devices: Seq[UserDevice], exchanges: Seq[ExchangeData])

object DeviceType extends Enumeration {
  val iPhone     = Value("iPhone")
  val iPad       = Value("iPad")
  val Android    = Value("android")
  val Web        = Value("web")
}


case class UserDevice(id: UUID, userId: UUID, deviceType: String, deviceId: String, deviceToken: String)

object UserDevice {
  def apply(userId: UUID, deviceType: String, deviceId: String, deviceToken: String): UserDevice = {
    UserDevice(UUID.randomUUID(), userId, deviceType, deviceId, deviceToken)
  }
  def applyWithId(id: UUID, userId: UUID, deviceType: String, deviceId: String, deviceToken: String): UserDevice = {
    UserDevice(id, userId, deviceType, deviceId, deviceToken)
  }
}

case class ExchangeData(apiKeyId: String, name: Exchange.Value, balances: Seq[Balance])

object UserData {
  def fromUser(user: User) = new UserData(user.id, user.firstName, user.lastName, user.email, Seq.empty, Seq.empty)
}
