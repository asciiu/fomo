package slick

import com.github.tminglei.slickpg._

trait MyPostgresDriver extends ExPostgresProfile
  with PgArraySupport
  with PgEnumSupport
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgCirceJsonSupport {

  override val api = new MyAPI {}

  override val pgjson = "jsonb"

  trait MyAPI extends API
    with ArrayImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants
    with JsonImplicits
}

object MyPostgresDriver extends MyPostgresDriver

