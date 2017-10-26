package com.flow.bittrex.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

case class BittrexTrade(id: UUID,
                         marketName: String,
                         isOpen: Boolean,
                         quantity: Double,
                         bidPrice: Double,
                         createdOn: OffsetDateTime,
                         purchasedPrice: Option[Double],
                         purchasedOn: Option[OffsetDateTime])


object BittrexTrade {
  def withRandomUUID(marketName: String,
                     isOpen: Boolean,
                     quantity: Double,
                     bidPrice: Double): BittrexTrade = {
    BittrexTrade(UUID.randomUUID(), marketName, true, quantity,bidPrice,
      Instant.now().atOffset(ZoneOffset.UTC), None, None)
  }
}
