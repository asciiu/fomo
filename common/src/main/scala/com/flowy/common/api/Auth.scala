package com.flowy.common.api

import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import play.api.libs.ws.StandaloneWSRequest

/**
  * Created by bishop on 9/7/16.
  */
case class Auth(val apiKeyId: UUID, val apiKey: String, secretKey: String) {

  require(secretKey.length > 0, "Secret cannot be empty")

  private def bytesToHex(bytes: Array[Byte]): String = {
    val hexArray = "0123456789ABCDEF".toCharArray
    val hexChars = new Array[Char](bytes.length * 2)
    var j = 0
    while ( {
      j < bytes.length
    }) {
      val v = bytes(j) & 0xFF
      hexChars(j * 2) = hexArray(v >>> 4)
      hexChars(j * 2 + 1) = hexArray(v & 0x0F)

      {
        j += 1
        j - 1
      }
    }
    new String(hexChars)
  }

  protected def generateSignature(uri: String): String = {
    val shaMac = Mac.getInstance("HmacSHA512")
    val keyspec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512")
    shaMac.init(keyspec)

    val macData = shaMac.doFinal(uri.getBytes())
    bytesToHex(macData)
  }

  def bittrexRequest(request: StandaloneWSRequest): StandaloneWSRequest = {
    val uri = request.uri.toString
    val signature = generateSignature(uri)

    request.addHttpHeaders("apisign" -> signature)
  }
}
