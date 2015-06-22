My first attempt of creating a CRUD application using Play 2.4 and Slick 3.0
Inspiration: http://semisafe.com/coding/2015/06/12/play_basics_database_access.html

What I've learned so far:

* `JsValue` is any valid Json tree
* `JsResult[T]` is any result of interpreting the JsValue as an object of type T, and it's kind of an Either, with cases JsSuccess[T] or JsError
* `JsResult[T]` has all the classic monadic transformations like Either, plus `asOpt[T]` and `asEither[T]`
* `JsValue` has three very convenient methods that allow avoiding using any verbose solution:
  * `jsValue.validate[T](implicit reads: Reads[T]): JsResult[T]`, therefore any type that provides a Reads in its c.o. works (Type Classes)
  * `jsValue.as[T](implicit reads: Reads[T])`
  * `jsValue.asOpt[T](implicit reads: Reads[T])`

