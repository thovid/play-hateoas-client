package de.thovid.play.hateoas.linkformat

import de.thovid.play.hateoas.Link
import de.thovid.play.hateoas.Links
import play.api.libs.json.Reads

trait LinkFormat {
  implicit val linkReads: Reads[Link]
  implicit val linksReads: Reads[Links]
}