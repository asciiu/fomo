package com.flowy.common.services

import slick.dbio.{DBIOAction, NoStream}


import scala.concurrent.Future


trait DBService {
  def runAsync[R](a: DBIOAction[R, NoStream, Nothing]): Future[R]

  def run[R](a: DBIOAction[R, NoStream, Nothing]): R

  //def stream[R](a: DBIOAction[_, Streaming[R], Nothing]): DatabasePublisher[R]
}

//class DBServiceImpl (val dbConfigProvider: DatabaseConfigProvider) extends DBService {
  //private val db = dbConfigProvider.get[JdbcProfile].db

 // def runAsync[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = {
    //db.run(a)
   // Future.successful("t")
 // }

 // def run[R](a: DBIOAction[R, NoStream, Nothing]): R = {
 //   Await.result(runAsync(a), Duration.Inf)
 // }

  //def stream[R](a: DBIOAction[_, Streaming[R], Nothing]): DatabasePublisher[R] = {
    //db.stream(a)
  //}
//}

