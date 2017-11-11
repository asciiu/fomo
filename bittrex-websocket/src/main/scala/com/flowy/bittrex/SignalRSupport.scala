package com.flowy.bittrex

import microsoft.aspnet.signalr.client.{SignalRFuture, Action, ErrorCallback}
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
  private lazy val connection = new HubConnection(signalRServiceUrl)

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

    if (configurator != null)
      configurator(connection)

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
    if (error != null) println(s"connection.onError: $error")
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

