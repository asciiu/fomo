package com.flowy.fomoApi.models

import java.time.OffsetDateTime
import java.util.UUID

case class RememberMeToken(id: UUID, selector: String, tokenHash: String, userId: UUID, validTo: OffsetDateTime)

