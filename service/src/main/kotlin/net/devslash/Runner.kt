package net.devslash

/**
 * Timeouts.
 * Use `0` to specify infinite.
 * Negative value mean to use the system's default value.
 */
@Suppress("MemberVisibilityCanBePrivate")
class ConfigBuilder {
  /**
   * Max time between TCP packets - default 10 seconds.
   */
  var followRedirects: Boolean = false

  /**
   * Max time to establish an HTTP connection - default 10 seconds.
   */
  var socketTimeout: Int = 10_000

  /**
   * Max time to establish an HTTP connection - default 20 seconds.
   */
  var connectTimeout: Int = 20_000

  /**
   * Max time for the connection manager to start a request - 20 seconds.
   */
  var connectionRequestTimeout: Int = 20_000

  fun build(): Config {
    return Config(followRedirects, socketTimeout, connectTimeout, connectionRequestTimeout)
  }
}

data class Config(
  val followRedirects: Boolean,
  val socketTimeout: Int,
  val connectTimeout: Int,
  val connectionRequestTimeout: Int
)

fun runHttp(block: SessionBuilder.() -> Unit) {
  return runHttp({}, block)
}

fun runHttp(config: ConfigBuilder.() -> Unit, block: SessionBuilder.() -> Unit) {
  val builtConfig = ConfigBuilder().apply(config).build()
  return runHttp(HttpDriver(KtorClientAdapter(builtConfig)), block)
}

internal fun runHttp(engine: Driver, block: SessionBuilder.() -> Unit) {
  val session = SessionBuilder().apply(block).build()
  HttpSessionManager(engine, session).run()
}
