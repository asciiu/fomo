package models

import java.util.UUID

import org.joda.time.DateTime

case class RememberMeToken( selector: String, tokenHash: String, userId: UUID, validTo: DateTime)
