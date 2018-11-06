package net.devslash

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

fun getCallDataSupplier(data: RequestDataSupplier?): RequestDataSupplier {
  if (data != null) {
    data.init()
    return data
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
