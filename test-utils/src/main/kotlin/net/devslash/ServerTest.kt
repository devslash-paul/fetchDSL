package net.devslash

import io.ktor.application.Application
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.junit.After
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

abstract class ServerTest {
  abstract var appEngine: ApplicationEngine
  protected val serverPort: Int = ServerSocket(0).use { it.localPort }
  protected val address: String = "http://localhost:$serverPort"

  fun start() {
    appEngine.start()
  }

  @After
  fun afterEach() {
    appEngine.stop(1, 1, TimeUnit.MILLISECONDS)
  }

  fun runWith(block: Application.() -> Unit) {
    appEngine = embeddedServer(Netty, port = serverPort, module = block)
    appEngine.start()
  }
}
