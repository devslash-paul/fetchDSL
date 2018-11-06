package net.devslash.examples

import com.sun.net.httpserver.HttpServer
import java.io.Closeable
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetSocketAddress

class EchoServer : Closeable {
  val server = HttpServer.create().apply {
    createContext("/") { http ->
      http.responseHeaders.add("Content-type", "text/plain")
      http.sendResponseHeaders(200, 0)
      PrintWriter(http.responseBody).use { out ->
        val requestBody = http.requestBody.bufferedReader().use { it.readText() }
        out.println(requestBody)
      }
    }
  }
  val address: String
    get() = "http://${server.address.hostString}:${server.address.port}"

  init {
    server.bind(InetSocketAddress(Inet4Address.getLocalHost(), 0), 0)
    server.start()
  }

  override fun close() {
    server.stop(0)
  }
}
