package controllers

import play.api._
import play.api.mvc._
import de.thovid.play.hateoas._
import play.api.libs.json.Json

object Application extends Controller {

  val samples = Map(
    "1" -> Sample("1", "First Sample"),
    "2" -> Sample("2", "Second Sample"))

  def list = Action { implicit request =>
    Ok(Json.obj(
      "samples" -> (samples map (s =>
        Json.obj(
          "id" -> s._1,
          "links" -> Json.arr(
            Link(rel = "self", path = routes.Application.get(s._1).absoluteURL(false))))))))
  }

  def get(id: String) = Action { implicit request =>
    samples.get(id) match {
      case Some(sample) =>
        Ok(Json.obj(
          "id" -> sample.id,
          "name" -> sample.name,
          "links" -> Json.arr(
            Link(rel = "self", path = routes.Application.get(id).absoluteURL(false)))))
      case None => NotFound
    }
  }

  def add = Action(BodyParsers.parse.json) { implicit request =>
    Ok
  }
}

case class Sample(id: String, name: String)