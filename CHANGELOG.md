0.20.0
======
Long time no see. This summarises since 0.19.3

* Calls have their own concurrency level set
* Body's can have an Inputstream rather than requiring a string
* action hooks now have the resolved data type
* A specific resolved hook exists 
* Url provider can be defined instead of a URL
* Editor config added

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
