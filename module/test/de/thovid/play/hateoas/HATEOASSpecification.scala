package de.thovid.play.hateoas

import scala.concurrent.ExecutionContext.Implicits.global

import org.specs2.mutable.Specification

import de.thovid.play.hateoas.linkformat.spring.SpringLinkFormat.linkWrites
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Handler
import play.api.mvc.Results.NotFound
import play.api.mvc.Results.Ok
import play.api.test.FakeApplication
import play.api.test.Helpers.OK
import play.api.test.WithServer

class HATEOASSpecification extends Specification {

  "HATEOAS Client" should {

    "get some json reponse" in new WithServer(app = TestApplication.app, port = TestApplication.port) {
      val result = HATEOAS.client
        .at(s"http://localhost:$port/samples/1")
        .get()
        .asJson {
          case (OK, json) => name(json)
        }
      result must beRight("First Sample").await
    }

    "follow a link selected from a list of entries" in new WithServer(app = TestApplication.app, port = TestApplication.port) {
      val result = HATEOAS.client
        .at(s"http://localhost:$port/samples")
        .following("self", selectedBy("samples" -> "id", "2"))
        .get()
        .asJson {
          case (OK, json) => name(json)
        }

      result must beRight("Second Sample").await
    }

    "not fail if result is not json" in new WithServer(app = TestApplication.app, port = TestApplication.port) {
      val result = HATEOAS.client
        .at(s"http://localhost:$port/errors/not-json")
        .get()
        .asJson {
          case (OK, json) => name(json)
        }

      result must beLeft.await
    }

    "not fail if endoint does not answer" in new WithServer(app = TestApplication.app, port = TestApplication.port) {
      val result = HATEOAS.client
        .at("http://does.not.exist")
        .get()
        .asJson {
          case (OK, json) => name(json)
        }

      result must beLeft.await
    }
  }

  private def name(json: JsValue): Either[String, String] = (json \ "name").asOpt[String] map (n => Right(n)) getOrElse (Left("nok"))
}

object TestApplication {

  val routes: PartialFunction[(String, String), Handler] = {
    case ("GET", "/samples") => TestApplication.list
    case ("GET", "/samples/1") => TestApplication.get("1")
    case ("GET", "/samples/2") => TestApplication.get("2")
    case ("GET", "/errors/not-json") => TestApplication.getNonJson
  }

  val port = 3333
  val app = FakeApplication(withRoutes = routes)

  val samples = Map(
    "1" -> Sample("1", "First Sample"),
    "2" -> Sample("2", "Second Sample"))

  def list = Action { implicit request =>
    Ok(Json.obj(
      "samples" -> (samples map (s =>
        Json.obj(
          "id" -> s._1,
          "links" -> Json.arr(
            Link(rel = "self", path = s"http://localhost:$port/samples/${s._1}")))))))
  }

  def get(id: String) = Action { implicit request =>
    samples.get(id) match {
      case Some(sample) =>
        Ok(Json.obj(
          "id" -> sample.id,
          "name" -> sample.name,
          "links" -> Json.arr(
            Link(rel = "self", path = s"http://localhost:$port/samples/$id"))))
      case None => NotFound
    }
  }

  def getNonJson = Action {
    implicit request =>
      Ok("hi, im not json")
  }
}

case class Sample(id: String, name: String)