package net.devslash

fun <T> getBodyProvider(call: Call<T>, data: RequestData<T>): Body {
  val body = call.body ?: return EmptyBody

  if (body.jsonObject !== null) {
    return JsonRequestBody(checkNotNull(body.jsonObject))
  }

  if (body.lazyJsonObject != null) {
    val lazyJsonLambda = checkNotNull(body.lazyJsonObject)
    val dataToSerialize = lazyJsonLambda(data)
    return JsonRequestBody(dataToSerialize)
  }

  if (body.formData != null) {
    val form = body.formMapper!!(checkNotNull(body.formData), data)
    return FormRequestBody(form)
  }

  if (body.bodyValue != null) {
    val sBody = body.bodyValueMapper!!(checkNotNull(body.bodyValue), data)
    return StringRequestBody(sBody)
  }

  if (body.rawValue != null) {
    return BytesRequestBody(checkNotNull(body.rawValue).invoke(data))
  }

  if (body.multipartForm != null) {
    return MultipartFormRequestBody(checkNotNull(body.multipartForm))
  }

  if (body.lazyMultipartForm != null) {
    val lazyMultipartForm = checkNotNull(body.lazyMultipartForm)
    return MultipartFormRequestBody(lazyMultipartForm(data))
  }

  return EmptyBody
}
