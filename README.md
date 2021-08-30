# fetchDSL

fetchDSL is a tool that aims to provide the ability to easily declaritively create a series of HTTP calls

[![Build Status](https://dev.azure.com/devslash/FetchDSL/_apis/build/status/Fetch%20DSL%20Master%20Build%20-%20devslash?branchName=master)](https://dev.azure.com/devslash/FetchDSL/_build/latest?definitionId=3&branchName=master)

### Uses

FetchDSL has been designed to be able to easily parallise thousands of HTTP calls with little to no effort from the
user. It also works wonderfully for once-off calls, especially when setup for that call is consistent between different
examples (think website login).

### Examples

A basic example that'll send a GET request to `http://example.com`

```kotlin
runHttp {
    call("http://example.com")
}
```

As simple as that you'll have a HTTP call. Of course, the same could have easily been done with cURL. So lets go onto
slightly more challenging problems.

#### GET multiple times

```kotlin
runHttp {
    call("http://example.com/!1!") {
        data = FileDataSuppler("inputfile")
    }
}
```

This will call `http://example.com/` one time for every line in `inputfile`. Not only that, but it'll replace `!1!` with
whatever is the first part of the line (by default split by spaces).

#### More Examples

For more examples, see the "examples" directory, or the docs at fetchDSL.dev.
