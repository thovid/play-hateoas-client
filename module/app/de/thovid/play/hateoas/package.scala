package de.thovid.play

import de.thovid.play.hateoas.Link
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads.functorReads
import play.api.libs.json.Writes
import play.api.libs.json.JsValue

package object hateoas {

  implicit lazy val linkReads: Reads[Link] = (
    (JsPath \ "rel").read[String] and
    (JsPath \ "path").read[String])((path, rel) => Link(path, rel))

  implicit lazy val linkWrites: Writes[Link] = new Writes[Link] {
    def writes(link: Link) = Json.obj(
      "rel" -> link.rel,
      "path" -> link.path)
  }

  def selectedBy[A](path: (String, String), value: A)(implicit reads: Reads[A]): LinkSelection = new LinkSelection {
    def select(json: JsValue): Either[String, Links] = {
      (json \ path._1).asOpt[JsValue]
        .map(element => selectedBy(path._2, value).select(element))
        .getOrElse(Left(s"error: no element with name ${path._1}"))
    }
  }

  def selectedBy[A](key: String, value: A)(implicit reads: Reads[A]): LinkSelection = new LinkSelection {
    def select(json: JsValue): Either[String, Links] = {
      json.asOpt[List[JsValue]]
        .flatMap { _.filter(element => (element \ key).asOpt[A] == Some(value)).headOption }
        .flatMap { element => (element \ "links").asOpt[List[Link]] } match {
          case Some(links) => Right(Links(links))
          case None => Left(s"error: no element with $key = $value")
        }
    }
  }

  def fromToplevel(): LinkSelection = new LinkSelection {
    def select(json: JsValue): Either[String, Links] = {
      (json \ "links").asOpt[List[Link]]
        .map(links => Right(Links(links)))
        .getOrElse(Left(s"error: no links found"))
    }
  }
}