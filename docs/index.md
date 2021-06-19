---
description: Cayman is a clean, responsive theme for GitHub Pages.
---

FetchDSL is a compact DSL that allows for fast, easy to maintain, HTTP request scripts.

The DSL runs using kotlin, ktor, and kotlinx-coroutines to provide highly parallel requests, without requiring many
resources to run quickly.

# Getting started

The `runHttp` context is the main entrypoint for the DSL. Each instance
of `runHttp` could be considered similar to an incognito browser window. 
That is, within the `runHttp` block, cookies and state may be shared, 
and as soon as the block is over, any future blocks start from a fresh
state.

Multiple `runHttp` blocks can be run at the same time in different threads.
There is no global state.

#### Making a simple call

To get started with a simple call, open the `runHttp` context, and create a `call`

```kotlin
runHttp {
  call("https://example.com/")
}
```

At it most simple, this will not output anything, or provide anything in the
request body. This is analogous to the following silent, suppressed curl call.

```bash
curl -s --output /dev/null example.com
```

#### Making multiple calls

Multiple calls can be added to a single `runHttp` block. Cookies are shared with all calls in a `runHttp` block.

```kotlin
runHttp {
  call("https://example.com/")
  call("https://example.org/")
  call("https://example.info/")
}
```

This is quite slow, encumbering, and not very useful.

One of the major advantages of _fetchDSL_ is its ability to perform many 
thousands of requests efficiently. Even when there's changes in each
request. To take advantage of this, we have to define a `DataSupplier`.

### Changing request method
TODO

### Adding a body

### Making multiple requests with Data Suppliers

The concept of data suppliers comes from the fact that most of the time 
when you require many requests to occur, there's usually something a 
little different each time.

For instance, it may be that you want to download all the HTML pages given
a sitemap. Each time you're simply doing a GET call and saving the response,
but the URL will change each time.

Data suppliers are the basis for providing the changed data to a request. 

In the following example, we'll be utilising the `FileDataSupplier`. This is a basic
line-by-line supplier. Where each line indicates a new request, with the arguments
for that request on the line.

For example, if we were trying to add many items to our shopping cart, we may 
have a file that contains each item.
```
fish
banana
tissues
...many more lines
```

Then, to create a POST request for each of those lines, you'd simply create the following

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

There's a few new things here, so lets go through them.

First of all, a call takes a second parameter which is a block. In kotlin this can be specified outside of the
parameters, so the convention is to place it after the closed parenthses. Within this, you may specify details about
this specific call.

First of all, we'll change the call from its default (a GET) to a POST by specfying the type.

Then, we set our data. The FileDataSupplier reads line by line from the input file.

Finally, we set the body. Note that the body, like the `call` receives a block as a parameter. There are many ways to
set a body, depending on your requirements. We choose the simplest, which just sets the body to specific text content.

We also see `"!1!"`. This is a replacement section and at the heart of `fetchDSL`. This allows you to specify something
that should be replaced on a per call basis. The way this works, is that the data supplier is called at the start of
each request, and returns a map of replacements. In the default case, the replacements are specified as `!1!`, `!2!`
, `!3!` and so on. A one based index wrapped in `!`. The replacements themselves come from the line in the supplied
file.

In a file such as

```
hello there
Bye now
```

And using a supplier such as `FileDataSupplier`. Then `!1!` would be replaced with `hello` on the first request.
Any `Bye` on the second.
`!2!` would be `there` on th first request, and `now` on the second.

`FileDataSupplier` allows you to split by more than just spaces. The optional second argument to `FileDataSupplier` is
the splitting char.

# Before Hooks

Sometimes you'll want something to take place _before_ your HTTP request gets sent. `fetchDSL` is bundled with a few
that should help, but you're encouraged to create your own as well.

#### SkipIf

The Skip hook is a bundled hook that can be utilised to figure out if a request should be avoided. This is useful in the
case where you have a large set of requests to occur any no simple way to pre-screen which ones should be re-attempted.
This is especially useful when you are dealing with a flaky endpoint that may require restarting your batch of requests.

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

There's quite a bit to unpack there. So lets start with the block statement. `before` indicates a series of things to
occur before a request.

The evaluation of the block occurs in a builder-style pattern, so simply writing code within the block will execute it
all before the first request is created. To have something that occurs every time it must be 'added' to the before set
of actions. This occurs via the `+`
symbol. You'll see this in `after` blocks as well.

To be a valid `+` target you must implement `BeforeHook`. As of now, this interface is empty, but fetchDSL knows how to
run the following specific children:

* `SimpleBeforeHook` - This allows you to see the incoming request and data. The request and data is unmodifiable but
  can be used for debugging, logging or other purposes
* `SessionPersistingBeforeHook` - This provides mutable references to the request. An immutable copy of the request
  data. It also provides a session manager and a cookie jar. Effectively this allows you to perform HTTP calls before
  the request being prepared but with the same cookie jar which optionally allows for keeping the request within the
  same context as the one being prepared. This is invaludable for things such as CSRFs where you must call something and
  then attach the token in the headers.
* `SkipBeforeHook` - This is a special hook that is always run before other hooks. If any `SkipBeforeHook`
  returns `true`, the request is terminated. Note that as soon as a single `SkipBeforeHook` returns `true`, all
  other `SkipBeforeHooks` are themselves skipped.

#### Once

The `Once` hook is simply a hook that itself takes another hook. That inner hook will only ever be performed once. Due
to an exclusive lock on the inner parts of the `Once` look, all other requests will block behind the `Once` block
finishing.

_Note_: Currently the `Once` hook is running an experiment where it will accept custom declarations of `BeforeHook`. It
will attempt to use reflection to find an appropriate set of parameters to provide to invoke the hook. This also means
that the hooks provided to the `Once` can be suspended. If an appropriate method could not be found,
an `InvalidHookException` is thrown.

#### LogRequest

This is a simple logging hook that logs to StdOut.

# After Hooks

### LogResponse

This simply outputs to StdOut. You are able to customise the output by providing an implementation to `OutputFormat` in
the optional second argument.

### Output

Often, you'll want to see the results of a call or store it in some way. In `f
