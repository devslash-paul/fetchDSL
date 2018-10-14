package net.devslash.runner

import net.devslash.Call
import net.devslash.DataSupplier

interface URLProvider {
  fun get(): String
}

interface BodyProvider {
  fun get(): String
}

interface OutputHandler {
  fun output(resp: HttpResponse, data: RequestData)
  fun suspectedOutput(data: RequestData): String? = null
}

class SystemOutputProvider : OutputHandler {
  override fun output(resp: HttpResponse, data: RequestData) {
    println("Call [${resp.url}] => ${resp.statusCode}")
  }
}

fun getOutputHandler(call: Call, data: RequestData): OutputHandler {
  if (call.output != null) {
    if (call.output.file != null) {
      return FileOutputProvider(call.output.file.name, call.output.file.append, call.output.binary)
    }
    if (call.output.consumer != null) {
      return object: OutputHandler {
        override fun output(resp: HttpResponse, data: RequestData) {
          call.output.consumer.accept(String(resp.body))
        }
      }
    }
    return SystemOutputProvider()
  }
  return object : OutputHandler {
    override fun output(resp: HttpResponse,
                        data: RequestData) = Unit
  }
}

fun getCallDataSupplier(data: DataSupplier?): RequestDataSupplier {
  if (data?.requestFile != null) {
    return FileBasedDataSupplier(data.requestFile)
  }
  return object : RequestDataSupplier {
    override fun hasNext(): Boolean {
      return false
    }

    override fun getDataForRequest(): RequestData {
      return object : RequestData {
        override fun getReplacements(): Map<String, String> {
          return mapOf()
        }
      }
    }
  }
}
