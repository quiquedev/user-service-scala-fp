package info.quiquedev.userservice.usecases

import doobie.util.meta.Meta
import io.circe._
import io.circe.parser._
import org.postgresql.util.PGobject

package object model {
  def jsonCoderOf[A: Encoder: Decoder]: Meta[A] = {
    val E = Encoder[A]
    val D = Decoder[A]

    Meta.Advanced
      .other[PGobject]("json")
      .imap[A](pgObject => {
        val json = parse(pgObject.getValue()).left
          .map(e => throw DbJsonCodingError(e))
          .merge
        D.decodeJson(json).left.map(e => throw DbJsonCodingError(e)).merge
      })(a => {
        val p = new PGobject

        p.setType("json")
        p.setValue(E.apply(a).noSpaces)
        p
      })
  }
}
