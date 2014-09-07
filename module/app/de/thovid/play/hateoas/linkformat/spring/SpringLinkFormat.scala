package de.thovid.play.hateoas.linkformat.spring

import de.thovid.play.hateoas.Link
import de.thovid.play.hateoas.Links
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsPath
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Reads._
import play.api.libs.json.Writes
import de.thovid.play.hateoas.linkformat.LinkFormat

object SpringLinkFormat extends LinkFormat {

  implicit lazy val linkReads: Reads[Link] = (
    (JsPath \ "rel").read[String] and
    (JsPath \ "path").read[String])((path, rel) => Link(path, rel))

  implicit lazy val linkWrites: Writes[Link] = new Writes[Link] {
    def writes(link: Link) = Json.obj(
      "rel" -> link.rel,
      "path" -> link.path)
  }

  implicit lazy val linksReads = new Reads[Links] {
    def reads(json: JsValue): JsResult[Links] = {
      (json \ "links").validate[List[Link]](Reads.list(linkReads)) map (links => Links(links))
    }
  }

}