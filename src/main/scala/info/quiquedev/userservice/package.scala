package info.quiquedev

package object userservice {
  implicit final class StringExtensions(val value: String) extends AnyVal {
    def nonNullOrEmpty: Boolean = Option(value).forall(!_.isEmpty)
  }
}
