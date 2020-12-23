package net.devslash

/**
 * Timeouts.
 * Use `0` to specify infinite.
 * Negative value mean to use the system's default value.
 */
public class ConfigBuilder {
  /**
   * Max time between TCP packets - default 10 seconds.
   */
  public var followRedirects: Boolean = false
  /**
   * Max time to establish an HTTP connection - default 10 seconds.
   */
  public var socketTimeout: Int = 10_000
  /**
   * Max time to establish an HTTP connection - default 10 seconds.
   */
  public var connectTimeout: Int = 20_000
  /**
   * Max time for the connection manager to start a request - 20 seconds.
   */
  public var connectionRequestTimeout: Int = 20_000

  internal fun build(): Config {
    return Config(followRedirects, socketTimeout, connectTimeout, connectionRequestTimeout)
  }
}

internal data class Config(val followRedirects: Boolean, val socketTimeout: Int, val connectTimeout: Int, val connectionRequestTimeout: Int)

public fun runHttp(block: SessionBuilder.() -> Unit) {
  return runHttp({}, block)
}

public fun runHttp(config: ConfigBuilder.() -> Unit, block: SessionBuilder.() -> Unit) {
  return runHttp(HttpDriver(ConfigBuilder().apply(config).build()), block)
}

internal fun runHttp(engine: HttpDriver, block: SessionBuilder.() -> Unit) {
  val session = SessionBuilder().apply(block).build()
  HttpSessionManager(engine, session).run()
}
