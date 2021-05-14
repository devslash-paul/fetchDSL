0.14.0
====

* Delay working
* Added chained calls
* ListDataSupplier now has a default transformer
* Http calling behavior has been abstracted to simplify testing

0.5.0
====

* Update header API to only require String. Bringing ReplaceableValue internally
* Change hooks to be before/after
* Removed output
* Added experimental hook API that allows for custom hooks to be provided, and injected in

0.4.1
=====

* Fix headers not being applied correctly.

0.4.0
====

* Changed to use ktor as the default client library. This highlighted the importance of being able to switch this out in
  the future
