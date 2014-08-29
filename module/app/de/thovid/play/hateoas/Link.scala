package de.thovid.play.hateoas

case class Link(rel: String, path: String)

case class Links(links: Map[String, Link]) {
  def merge(other: Links): Links = Links(this.links ++ other.links)
  def get(rel: String): Option[Link] = links.get(rel)
}

object Links {
  def apply(links: List[Link]): Links = {
    val map = links.foldLeft(Map[String, Link]())((r, e) => r + (e.rel -> e))
    Links(map)
  }
  def apply(link: Link): Links = apply(List(link))

  def apply(): Links = Links(List())
}
