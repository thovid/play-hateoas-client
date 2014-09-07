package de.thovid.play

import de.thovid.play.hateoas.LinkSelection
import de.thovid.play.hateoas.Links
import de.thovid.play.hateoas.linkformat.LinkFormat
import play.api.libs.json.JsValue
import play.api.libs.json.Reads

package object hateoas {

  def selectedBy[A](path: (String, String), value: A)(implicit reads: Reads[A]): LinkSelection = new LinkSelection {
    def select(json: JsValue, linkFormat: LinkFormat): Either[String, Links] = {
      (json \ path._1).asOpt[JsValue]
        .map(element => selectedBy(path._2, value).select(element, linkFormat))
        .getOrElse(Left(s"error: no element with name ${path._1}"))
    }
  }

  def selectedBy[A](key: String, value: A)(implicit reads: Reads[A]): LinkSelection = new LinkSelection {
    def select(json: JsValue, linkFormat: LinkFormat): Either[String, Links] = {
      import linkFormat._

      json.asOpt[List[JsValue]]
        .flatMap { _.filter(element => (element \ key).asOpt[A] == Some(value)).headOption }
        .flatMap { element => element.asOpt[Links] } match {
          case Some(links) => Right(links)
          case None => Left(s"error: no element with $key = $value")
        }
    }
  }

  def fromToplevel(): LinkSelection = new LinkSelection {
    def select(json: JsValue, linkFormat: LinkFormat): Either[String, Links] = {
      import linkFormat._

      json.asOpt[Links]
        .map(links => Right(links))
        .getOrElse(Left(s"error: no links found"))
    }
  }
}