package net.devslash

import java.util.function.Consumer

@DslMarker
annotation class SessionDsl

enum class HttpMethod {
  GET, POST
}

class HookBuilder<T> {

  private var hooks = mutableListOf<T>()

  operator fun T.unaryPlus() {
    hooks = (hooks + this).toMutableList()
  }

  fun build(): MutableList<T> {
    return hooks
  }
}

@SessionDsl
open class CallBuilder(private val url: String) {
  private var cookieJar: String? = null
  private var output: Output? = null
  private var dataSupplier: DataSupplier? = null
  var body: HttpBody? = null
  var type: HttpMethod = HttpMethod.GET
  var headers: List<Pair<String, ReplaceableValue<String, RequestData>>>? = null
  private var skipRequestIfOutputExists: Boolean = false

  // new style
  private var preHooksList = mutableListOf<PreHook>()
  private var postHooksList = mutableListOf<PostHook>()

  fun preHook(block: HookBuilder<PreHook>.() -> Unit) {
    preHooksList = HookBuilder<PreHook>().apply(block).build()
  }

  fun postHook(block: HookBuilder<PostHook>.() -> Unit) {
    postHooksList = HookBuilder<PostHook>().apply(block).build()
  }

  fun output(block: OutputBuilder.() -> Unit) {
    output = OutputBuilder().apply(block).build()
  }

  fun data(block: DataSupplierBuilder.() -> Unit) {
    dataSupplier = DataSupplierBuilder().apply(block).build()
  }

  fun body(block: BodyBuilder.() -> Unit) {
    body = BodyBuilder().apply(block).build()
  }

  fun build(): Call {
    return Call(url, headers, cookieJar, output, type, dataSupplier, body,
        skipRequestIfOutputExists, preHooksList, postHooksList)
  }
}

@SessionDsl
class BodyBuilder {
  var value: String? = null
  var formParams: List<Pair<String, String>>? = null

  fun build(): HttpBody = HttpBody(value, formParams)
}

@SessionDsl
class DataSupplierBuilder {
  private var file: InputFile? = null

  fun file(block: InputFileBuilder.() -> Unit) {
    file = InputFileBuilder().apply(block).build()
  }

  fun build(): DataSupplier = DataSupplier(file)
}


@SessionDsl
class SessionBuilder {
  private var calls = mutableListOf<Call>()
  var concurrency = 20

  fun call(url: String, block: CallBuilder.() -> Unit = {}) {
    calls.add(CallBuilder(url).apply(block).build())
  }

  fun build(): Session = Session(calls, concurrency)
}

@SessionDsl
class OutputBuilder {
  var file: OutputFile? = null
  var consumer: Consumer<String>? = null
  var pprintJson: Boolean = true
  var binary: Boolean = false

  fun file(block: FileBuilder.() -> Unit) {
    file = FileBuilder().apply(block).build()
  }

  fun build(): Output = Output(file, consumer, pprintJson, binary)
}

@SessionDsl
class InputFileBuilder {
  var name: String? = null
  var split: String = " "

  fun build(): InputFile = InputFile(name!!, split)
}

@SessionDsl
class FileBuilder {
  var name: String? = null
  var append: Boolean = false
  var perLine: Boolean = false
  var split: String = " "

  fun build(): OutputFile = OutputFile(name!!, append, split, perLine)
}

