package com.softwaremill.bootzooka.common.api

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.model.headers.{`Cache-Control`, `Last-Modified`, _}
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.flowy.common.models._
import com.softwaremill.bootzooka.common.api.`X-Content-Type-Options`.`nosniff`
import com.softwaremill.bootzooka.common.api.`X-Frame-Options`.`DENY`
import com.softwaremill.bootzooka.common.api.`X-XSS-Protection`.`1; mode=block`
import io.circe._
import io.circe.jawn.decode

trait RoutesSupport extends JsonSupport {

  object JsonStatus extends Enumeration {
    val Success = Value("success")
    val Fail    = Value("fail")
    val Error   = Value("error")
  }

  case class JsonKey(value: String)
  case class JSendResponse(status: JsonStatus.Value, message: String, data: Json)

  // required by circe
  implicit val jsonStanardReponse = CanBeSerialized[JSendResponse]
  implicit val jsonStatus = CanBeSerialized[JsonStatus.Value]

  implicit val JsonKeyEncoder = new KeyEncoder[JsonKey] {
    override def apply(key: JsonKey): String = key.value
  }

  implicit val encodeJSend: Encoder[JSendResponse] = new Encoder[JSendResponse] {
    final def apply(a: JSendResponse): Json = Json.obj(
      ("status", Json.fromString(a.status.toString)),
      ("message", Json.fromString(a.message)),
      ("data", a.data)
    )
  }

  implicit val decodeJSVal: Decoder[JsonStatus.Value] =
      Decoder.decodeString.map(JsonStatus.withName)

  implicit val decodeJSSend: Decoder[JSendResponse] =
    Decoder.forProduct3("status", "message", "data")(JSendResponse.apply)

  //def completeOk = complete("ok")
  def completeOk = complete(StatusCodes.OK, JSendResponse(JsonStatus.Success, "", Json.Null))


  implicit val encodeStatus: Encoder[ApiKeyStatus.Value] = new Encoder[ApiKeyStatus.Value] {
    final def apply(a: ApiKeyStatus.Value): Json = Json.fromString(a.toString)
  }
  implicit val encodeExchange: Encoder[Exchange.Value] = new Encoder[Exchange.Value] {
    final def apply(a: Exchange.Value): Json = Json.fromString(a.toString)
  }
  implicit val deviceType: Encoder[DeviceType.Value] = new Encoder[DeviceType.Value] {
    final def apply(a: DeviceType.Value): Json = Json.fromString(a.toString)
  }
  implicit val actionType: Encoder[TradeAction.Value] = new Encoder[TradeAction.Value] {
    final def apply(a: TradeAction.Value): Json = Json.fromString(a.toString)
  }


  implicit val basicUserDataCbs = CanBeSerialized[UserData]
}

trait JsonSupport extends CirceEncoders {

  implicit def materializer: Materializer

  implicit def circeUnmarshaller[A <: Product: Manifest](implicit d: Decoder[A]): FromEntityUnmarshaller[A] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .mapWithCharset { (data, charset) =>
        val input =
          if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
        decode[A](input) match {
          case Right(obj)    => obj
          case Left(failure) => throw new IllegalArgumentException(failure.getMessage, failure.getCause)
        }
      }

  implicit def circeMarshaller[A <: AnyRef](implicit e: Encoder[A], cbs: CanBeSerialized[A]): ToEntityMarshaller[A] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`application/json`) {
      e(_).noSpaces
    }

  /**
    * To limit what data can be serialized to the client, only classes of type `T` for which an implicit
    * `CanBeSerialized[T]` value is in scope will be allowed. You only need to provide an implicit for the base value,
    * any containers like `List` or `Option` will be automatically supported.
    */
  trait CanBeSerialized[T]

  object CanBeSerialized {
    def apply[T]                                                                                        = new CanBeSerialized[T] {}
    implicit def listCanBeSerialized[T](implicit cbs: CanBeSerialized[T]): CanBeSerialized[List[T]]     = null
    implicit def setCanBeSerialized[T](implicit cbs: CanBeSerialized[T]): CanBeSerialized[Set[T]]       = null
    implicit def optionCanBeSerialized[T](implicit cbs: CanBeSerialized[T]): CanBeSerialized[Option[T]] = null
  }

}

trait CacheSupport {

  import akka.http.scaladsl.model.DateTime

  private val doNotCacheResponse = respondWithHeaders(
    `Last-Modified`(DateTime.now),
    `Expires`(DateTime.now),
    `Cache-Control`(`no-cache`, `no-store`, `must-revalidate`, `max-age`(0))
  )
  private val cacheSeconds = 60L * 60L * 24L * 30L
  private val cacheResponse = respondWithHeaders(
    `Expires`(DateTime(System.currentTimeMillis() + cacheSeconds * 1000L)),
    `Cache-Control`(`public`, `max-age`(cacheSeconds))
  )

  private def extensionTest(ext: String): Directive1[String] = pathSuffixTest((".*\\." + ext + "$").r)

  private def extensionsTest(exts: String*): Directive1[String] = exts.map(extensionTest).reduceLeft(_ | _)

  val cacheImages =
    extensionsTest("png", "svg", "gif", "woff", "jpg").flatMap { _ =>
      cacheResponse
    } |
      doNotCacheResponse
}

trait SecuritySupport {
  val addSecurityHeaders = respondWithHeaders(
    `X-Frame-Options`(`DENY`),
    `X-Content-Type-Options`(`nosniff`),
    `X-XSS-Protection`(`1; mode=block`)
  )
}
