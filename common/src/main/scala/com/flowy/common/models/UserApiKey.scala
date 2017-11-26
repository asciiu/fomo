package com.flowy.common.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

case class UserKey (id: UUID,
                    userId: UUID,
                    exchange: Exchange.Value,
                    key: String,
                    secret: String,
                    description: String,
                    status: ApiKeyStatus.Value,
                    createdOn: OffsetDateTime,
                    updatedOn: OffsetDateTime
                   )


object UserKey  {
  def withRandomUUID(userId: UUID,
                     exchange: Exchange.Value,
                     key: String,
                     secret: String,
                     description: String,
                     status: ApiKeyStatus.Value) =
    UserKey(UUID.randomUUID(),
      userId,
      exchange,
      key,
      secret,
      description,
      status,
      Instant.now().atOffset(ZoneOffset.UTC),
      Instant.now().atOffset(ZoneOffset.UTC))
}
