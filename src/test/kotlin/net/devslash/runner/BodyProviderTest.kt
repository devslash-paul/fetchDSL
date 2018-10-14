//package net.devslash.runner
//
//import net.devslash.HttpBody
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
//
//internal class BodyProviderTest {
//
//  @Test fun testEmpty() {
//    val provider = getBodyProvider(getCall(), requestDataFromList(listOf("b", "d")))
//
//    assertEquals("", provider.get())
//  }
//
//
//  @Test fun testWithBody() {
//    val provider = getBodyProvider(getCall(HttpBody(null, listOf("a" to "b", "c" to "d"))),
//        requestDataFromList())
//
//    assertEquals("a=b&c=d", provider.get())
//  }
//
//  @Test fun testBodyWithReplaceableValues() {
//    val provider = getBodyProvider(getCall(HttpBody("a=!1!&c=!2!", null)),
//        requestDataFromList(listOf("b", "d")))
//    assertEquals("a=b&c=d", provider.get())
//  }
//
//  @Test fun testParamsWithReplacement() {
//    val provider = getBodyProvider(getCall(HttpBody(null, listOf("a" to "!1!", "c" to "!2!"))),
//        requestDataFromList(listOf("b", "d")))
//
//    assertEquals("a=b&c=d", provider.get())
//  }
//}
//
