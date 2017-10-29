package slick

import com.github.tminglei.slickpg._

trait MyPostgresDriver extends ExPostgresProfile
  with PgArraySupport
  with PgEnumSupport
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgSprayJsonSupport {

  override val api = new MyAPI {}

  override val pgjson = "jsonb"

  trait MyAPI extends API
    with ArrayImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants
    with SprayJsonPlainImplicits
    with JsonImplicits
}

object MyPostgresDriver extends MyPostgresDriver

