package net.devslash

interface URLProvider {
  fun get(): String
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
