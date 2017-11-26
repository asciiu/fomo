package com.flowy.common.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

case class UserKey (id: UUID,
                    userId: UUID,
                    exchange: String,
                    key: String,
                    secret: String,
                    description: String,
                    createdOn: OffsetDateTime,
                    updatedOn: OffsetDateTime,
                    validatedOn: Option[OffsetDateTime]
                   )


object UserKey  {
  def withRandomUUID(userId: UUID,
                     exchange: String,
                     key: String,
                     secret: String,
                     description: String
                    ) =
    UserKey(UUID.randomUUID(),
      userId,
      exchange,
      key,
      secret,
      description,
      Instant.now().atOffset(ZoneOffset.UTC),
      Instant.now().atOffset(ZoneOffset.UTC),
      Some(Instant.now().atOffset(ZoneOffset.UTC))
    )
}
