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

fun getOutputHandler(call: Call): OutputHandler {
//  if (call.output != null) {
//    val output = call.output!!
//    if (output.file != null) {
//      val file = output.file!!
//      return FileOutputProvider(file.name, file.append, output.binary)
//    }
//    if (call.output!!.consumer != null) {
//      return object: OutputHandler {
//        override fun output(resp: HttpResponse, data: RequestData) {
//          call.output!!.consumer!!.accept(String(resp.body))
//        }
//      }
//    }
//    return SystemOutputProvider()
//  }
  return object : OutputHandler {
    override fun output(resp: HttpResponse,
                        data: RequestData) = Unit
  }
}

fun getCallDataSupplier(data: DataSupplier?): RequestDataSupplier {
  if (data?.rds != null) {
    return data.rds!!
  }
  if (data?.requestFile != null) {
    return FileBasedDataSupplier(data.requestFile!!)
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
