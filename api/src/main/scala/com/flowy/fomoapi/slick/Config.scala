package com.flowy.fomoapi.slick

import com.flowy.common.slick.MyPostgresDriver

object Config{
  val jdbcDriver =  "org.postgresql.Driver"
  val slickProfile = MyPostgresDriver
}

