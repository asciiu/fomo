package models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import models.User.encryptPassword


case class UserKey (id: UUID,
                    userId: UUID,
                    key: String,
                    secret: String,
                    description: String,
                    createdOn: OffsetDateTime,
                    updatedOn: OffsetDateTime)


object UserKey  {
  def withRandomUUID(userId: UUID,
                     key: String,
                     secret: String,
                     description: String) =
    UserKey(UUID.randomUUID(),
      userId,
      key,
      secret,
      description,
      Instant.now().atOffset(ZoneOffset.UTC),
      Instant.now().atOffset(ZoneOffset.UTC)
    )
}