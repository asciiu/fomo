package com.flowy.bfeed

import java.util

import microsoft.aspnet.signalr.client.http.Request
import microsoft.aspnet.signalr.client.{Action, Credentials, ErrorCallback, SignalRFuture}
import microsoft.aspnet.signalr.client.hubs.HubConnection
import scala.concurrent.{Future, Promise}
import scala.util.Success

/**
 * Basic support for SignalR.
 * @note `connection` is pre-configured to call
 */
trait SignalRSupport {
  /**
   * The URL of the SignalR service to connect to.
   * @note AF: This is shitty design.
   *       Find a better way (concurrent dictionary of connections keyed by URL perhaps).
   */
  val signalRServiceUrl: String

  /**
   * The connection to SignalR.
   * @note AF: This is shitty design.
   *       Find a better way (concurrent dictionary of connections keyed by URL perhaps).
   */
  private lazy val connection = new HubConnection(signalRServiceUrl, false)

  /**
   * Asynchronously connect to the SignalR service specified by `signalRServiceUrl`.
   * @param configurator An optional function used to perform additional configuration of the connection.
   * @return A `Future[Void]` representing the asynchronous connection process.
   */
  protected def connectSignalR(configurator: (HubConnection) => Unit = null): Future[Unit] = {

    connection.error(new ErrorCallback {
      override def onError(error: Throwable): Unit = {
        onSignalRError(error)
      }
    })

    if (configurator != null) {
      configurator(connection)
    }
    // TODO this needs to be implemented for cloudfare
    // this did not work
    // https://github.com/n0mad01/node.bittrex.api/issues/67
    //val credentials = new Credentials() {
    //  override def prepareRequest(request: Request) {
    //    request.setHeaders(Map(
    //      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36",
    //      "cookie" -> "__cfduid=d5e3524ac98f70650dadb928afdf68a161511935946"
    //    ))
    //  }
    //}
    //connection.setCredentials(credentials)

    connection.start().toNativeFuture
  }

  /**
   * Disconnect from the SignalR service.
   */
  protected def disconnectSignalR(): Unit = {
    connection.stop()
  }

  /**
   * Called when the SignalR connection encounters an error.
   * @param error The error.
   */
  protected def onSignalRError(error: Throwable): Unit = {
    if (error != null) println(s"connection.onError: ${error}")
    else println("SignalR error handler: error was null!")
  }

  /**
   * Extension-method-style converter from SignalR futures to native (Scala) futures.
   * @param signalRFuture The SignalR future converter.
   * @tparam TResult The future result type.
   */
  implicit protected class SignalRFutureConverter[TResult](private val signalRFuture: SignalRFuture[TResult]) {
    def toNativeFuture: Future[TResult] = {
      if (signalRFuture == null)
        throw new IllegalArgumentException("SignalR future cannot be null.")

      val nativePromise = Promise[TResult]()

      // AF: Doesn't handle "cancellation" feature of SignalR futures.
      signalRFuture
          .done(new Action[TResult] {
            override def run(result: TResult): Unit = {
              nativePromise.complete(Success(result))
            }
          })
          .onError(new ErrorCallback {
            override def onError(error: Throwable): Unit = {
              nativePromise.failure(error)
            }
          })

      nativePromise.future
    }
  }

  /**
   * Extension-method-style converter from SignalR futures to native (Scala) futures.
   * @param signalRFuture The SignalR future converter.
   */
  implicit protected class SignalRVoidFutureConverter(private val signalRFuture: SignalRFuture[Void]) {
    def toNativeFuture: Future[Unit] = {
      if (signalRFuture == null)
        throw new IllegalArgumentException("SignalR future cannot be null.")

      val nativePromise = Promise[Unit]()

      // AF: Doesn't handle "cancellation" feature of SignalR futures.
      signalRFuture
          .done(new Action[Void] {
            override def run(result: Void): Unit = {
              nativePromise.trySuccess(result)
            }
          })
          .onError(new ErrorCallback {
            override def onError(error: Throwable): Unit = {
              nativePromise.tryFailure(error)
            }
          })

      nativePromise.future
    }
  }
}

