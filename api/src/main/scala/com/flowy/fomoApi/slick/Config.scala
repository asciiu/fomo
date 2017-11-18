package com.flowy.fomoApi.slick

import com.flowy.marketmaker.slick.MyPostgresDriver

object Config{
  val jdbcDriver =  "org.postgresql.Driver"
  val slickProfile = MyPostgresDriver
}

