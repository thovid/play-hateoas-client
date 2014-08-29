package de.thovid.play.hateoas

import org.specs2.mutable.Specification

import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper

class LinkSelectionSpecification extends Specification {

  "selection from array by path" should {
    "select single element with id" in {
      val json = Json.arr(
        Json.obj(
          "id" -> "1",
          "links" -> Json.arr(
            Json.obj("rel" -> "test", "path" -> "test/path/1"))),
        Json.obj(
          "id" -> "2",
          "links" -> Json.arr(
            Json.obj("rel" -> "test", "path" -> "test/path/2"))))

      selectedBy("id", "2")
        .select(json) must beRight(Links(Link(rel = "test", path = "test/path/2")))
    }

    "produce error if element with requested id is not found" in {
      val json = Json.arr(
        Json.obj(
          "id" -> "1",
          "links" -> Json.arr(
            Json.obj("rel" -> "test", "path" -> "test/path/1"))))

      selectedBy("id", "2")
        .select(json) must beLeft("error: no element with id = 2")
    }

    "select element from named array" in {
      val json = Json.obj(
        "elements" -> Json.arr(
          Json.obj(
            "id" -> "1",
            "links" -> Json.arr(
              Json.obj("rel" -> "test", "path" -> "test/path/1"))),
          Json.obj(
            "id" -> "2",
            "links" -> Json.arr(
              Json.obj("rel" -> "test", "path" -> "test/path/2")))))

      selectedBy("elements" -> "id", "2")
        .select(json) must beRight(Links(Link(rel = "test", path = "test/path/2")))
    }

    "produce error if named array not found" in {
      val json = Json.obj(
        "elements" -> Json.arr(
          Json.obj(
            "id" -> "1",
            "links" -> Json.arr(
              Json.obj("rel" -> "test", "path" -> "test/path/1")))))

      selectedBy("stuff" -> "id", "1")
        .select(json) must beLeft("error: no element with name stuff")
    }

    "be able to select by integer id" in {
      val json = Json.arr(
        Json.obj(
          "id" -> 42,
          "links" -> Json.arr(
            Json.obj("rel" -> "test", "path" -> "test/path/1"))),
        Json.obj(
          "id" -> 43,
          "links" -> Json.arr(
            Json.obj("rel" -> "test", "path" -> "test/path/2"))))

      selectedBy("id", 43)
        .select(json) must beRight(Links(Link(rel = "test", path = "test/path/2")))
    }
  }

  "selection from toplevel links" should {
    "select links" in {
      val json = Json.obj(
        "something" -> "ignore",
        "links" -> Json.arr(
          Json.obj("rel" -> "test", "path" -> "test/path")))

      fromToplevel()
        .select(json) must beRight(Links(Link(rel = "test", path = "test/path")))
    }
  }
}