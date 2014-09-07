package de.thovid.play.hateoas

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class SpringLinkFormatSpecification extends Specification {

  val json = Json.obj(
    "value" -> 42,
    "links" -> Json.arr(Json.obj(
      "rel" -> "self",
      "href" -> "some.url.com")))

  "link reader" should {
    import de.thovid.play.hateoas.linkformat.spring.SpringLinkFormat._

    "read link in spring format" in {

      (json \ "links").as[List[Link]] must be equalTo (List(Link("self", "some.url.com")))
    }
  }

}