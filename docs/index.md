---
[comment]: <> (#description: FetchD)
---

FetchDSL is a compact DSL that allows for fast, concurrent, HTTP requests.

# Uses

fetchDSL's main use case is for providing repeatable declarative HTTP requests.
In particular, it's built for safely handling thousands of requests in an
efficient manner while allowing control over request rate, request level changes
(url, body, headers, etc), as well as once off session level setup such as
login.

A simple example of a DSL scrip that may log in, and post potentially thousands
of unique POST requests is as follows. This script would log in once, then for
each line in `requests.txt` send a POST request. It would then print the
response body. All of this can be customised to your needs

```kotlin
runHttp {
  call("https://www.example.com") {
    type = POST
    data = FileDataSupplier("requests.txt")
    before {
      +Once(Login())
      +LogRequest()
    }
    body {
      value("Example Body")
    }
    after {
      action {
        println("The body response was ${resp.body}")
      }
    }
  }
}
```

# Getting started

`runHttp` context is the main entrypoint for the DSL. Each instance of `runHttp`
could be considered similar to a new browser window.

Multiple `runHttp` blocks can be run at the same time in different threads.
There is no shared global state.

## Making a call

To get started with a simple call, open the `runHttp` context, and create
a `call`

```kotlin
runHttp {
  call("https://example.com/")
}
```

At it most simple, this will not output anything, and will only do a GET call to
the provided url. This is analogous to the following silent, `curl` call.

```bash
curl -s --output /dev/null example.com
```

## Making multiple calls

There are a few possible ways to make multiple calls. Either explicitly, or
providing data parameters to a `call`.

The simplest form of multiple HTTP calls is to add new `call` statements. Each
`call` will be run one after each other. The second `call` is only started once
the previous one has returned

```kotlin
runHttp {
  call("https://example.com/")
  call("https://example.org/")
  call("https://example.info/")
}
```

## Customising the call

Just doing a GET call to a URL isn't particularly interesting. Every call can
change its `method`, `body`, `headers`, as well as set of hooks for before and
after the request takes places.

To take advantage of this customisation, one must supply a receiving block as
the second parameter of the `call`. In Kotlin this can take place via a block
defined after the parenthesis.

```kotlin
runHttp {
  call("http://fetchdsl.dev") {
    type = HttpMethod.POST
    headers = mapOf("X-Custom-Header" to listOf("value-1", "value-2"))
    body {
      value("Custom Body")
    }
    before {
      action {
        println("Preparing a request")
      }
    }
    after {
      action {
        println("Response was ${resp.statusCode}")
      }
    }
  }
}
```

Rather than just a simple GET request, we now have a POST that contains custom
headers, a body, and will print out a message prior to the request - as well as
the status code after the response has been returned.

### Making multiple requests with Data Suppliers

When repeating requests, it's common that something is a little different about
each request. For instance, the URL may change slightly, or the body of a form
request may be referring to something slightly different. A Data supplier is
used to supply the small change between each of these requests.

Supplying `data` to a call block allows for specific control over both the
number of requests that will take place, and the particular details about that
call.

There are a number of inbuilt data suppliers (and extra ones can be created via
implementing the `DataSupplier` interface). In the following example, we'll be
utilising the `FileDataSupplier`. This is a basic line based supplier. Where
each line from the file indicates one request, with the arguments for that
request on the line.

For example, if we were trying to add many items to our shopping cart, we may
have a file that contains each item.

```
fish
banana
tissues
...many more lines
```

Then, to create a POST request for each of those lines, you'd create the
following

```kotlin
runHttp {
  call("https://www.coolshoppingcart.com/addItem") {
    type = HttpMethod.POST
    data = FileDataSupplier("inputfile.txt")
    body {
      value("!1!")
    }
  }
}
```



### Inbuilt use for Data

We also see `"!1!"`. This is a replacement section and a way to customise each
request in `fetchDSL`. This allows you to specify something that should be
replaced on a per call basis. The way this works, is that the data supplier is
called at the start of each request, and returns a map of replacements. In the
default case, the replacements are specified as
`!1!`, `!2!`, `!3!` and so on. A one based index wrapped in `!`. The
replacements themselves come from the line in the supplied file.

In a file such as

```
hello there
Bye now
```

And using a supplier such as `FileDataSupplier`. Then `!1!` would be replaced
with `hello` on the first request. Any `Bye` on the second.
`!2!` would be `there` on th first request, and `now` on the second.

`FileDataSupplier` allows you to split by more than just spaces. The optional
second argument to `FileDataSupplier` is the splitting char.

### Adding a body

# Before Hooks

Sometimes you'll want something to take place _before_ your HTTP request gets
sent. `fetchDSL` is bundled with a few that should help, but you're encouraged
to create your own as well.

#### SkipIf

The Skip hook is a bundled hook that can be utilised to figure out if a request
should be avoided. This is useful in the case where you have a large set of
requests to occur any no simple way to pre-screen which ones should be
re-attempted. This is especially useful when you are dealing with a flaky
endpoint that may require restarting your batch of requests.

```kotlin
val cart = getCurrentShoppingCart()

runHttp {
  call("https://www.coolshoppingcart.com/addItem") {
    before {
      +SkipIf { data -> card.contains("!1!".asReplaceableValue().get(data)) }
    }
  }
}

```

There's quite a bit to unpack there. So lets start with the block
statement. `before` indicates a series of things to occur before a request.

The evaluation of the block occurs in a builder-style pattern, so simply writing
code within the block will execute it all before the first request is created.
To have something that occurs every time it must be 'added' to the before set of
actions. This occurs via the `+`
symbol. You'll see this in `after` blocks as well.

To be a valid `+` target you must implement `BeforeHook`. As of now, this
interface is empty, but fetchDSL knows how to run the following specific
children:

* `SimpleBeforeHook` - This allows you to see the incoming request and data. The
  request and data is unmodifiable but can be used for debugging, logging or
  other purposes
* `SessionPersistingBeforeHook` - This provides mutable references to the
  request. An immutable copy of the request data. It also provides a session
  manager and a cookie jar. Effectively this allows you to perform HTTP calls
  before the request being prepared but with the same cookie jar which
  optionally allows for keeping the request within the same context as the one
  being prepared. This is invaludable for things such as CSRFs where you must
  call something and then attach the token in the headers.
* `SkipBeforeHook` - This is a special hook that is always run before other
  hooks. If any `SkipBeforeHook`
  returns `true`, the request is terminated. Note that as soon as a
  single `SkipBeforeHook` returns `true`, all other `SkipBeforeHooks` are
  themselves skipped.

#### Once

The `Once` hook is simply a hook that itself takes another hook. That inner hook
will only ever be performed once. Due to an exclusive lock on the inner parts of
the `Once` look, all other requests will block behind the `Once` block
finishing.

_Note_: Currently the `Once` hook is running an experiment where it will accept
custom declarations of `BeforeHook`. It will attempt to use reflection to find
an appropriate set of parameters to provide to invoke the hook. This also means
that the hooks provided to the `Once` can be suspended. If an appropriate method
could not be found, an `InvalidHookException` is thrown.

#### LogRequest

This is a simple logging hook that logs to StdOut.

# After Hooks

### LogResponse

This simply outputs to StdOut. You are able to customise the output by providing
an implementation to `OutputFormat` in the optional second argument.

### Output

Often, you'll want to see the results of a call or store it in some way. In `f
