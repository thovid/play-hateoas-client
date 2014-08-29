package de.thovid.play.hateoas.implementation

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import de.thovid.play.hateoas.Link
import de.thovid.play.hateoas.LinkSelection
import de.thovid.play.hateoas.Links
import de.thovid.play.hateoas.RESTRequest
import de.thovid.play.hateoas.RESTResponse
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.ws.Response

class RESTRequestSpecification extends Specification with Mockito {

  val url = "http://some.url/some/stuff"

  "single service call" should {

    "returns json response if status code was 200" in {
      val service: PlayHATEOASClient = mock[PlayHATEOASClient].executeGet(url) returns Future.successful(Right(200, Json.obj()))

      val result = new SingleCallRequest(service, url).get
      result.asJson { case (200, json) => Right(true) } must beRight(true).await
    }

    "returns error if status code was not 200" in {
      val serviceResponse: Response = mock[Response].status returns 404
      val service: PlayHATEOASClient = mock[PlayHATEOASClient].executeGet(url) returns Future.successful(Left("error"))

      val result = new SingleCallRequest(service, url).get
      result.asJson { case (200, json) => Right(true) } must beLeft.await
    }
  }

  "chained service call" should {
    "call parent" in {
      val parent: RESTRequest = mock[RESTRequest]
        .get returns new RESTResponse(Future.successful(Right(200, Json.obj("ok" -> false))))
      val service: PlayHATEOASClient = mock[PlayHATEOASClient]
        .executeGet(url) returns Future.successful(Right(200, Json.obj("ok" -> true)))
      val linkSelection: LinkSelection = mock[LinkSelection]
        .select(any) returns (Right(Links(Link(rel = "link", path = url))))

      val result = new ChainedCallRequest(service, "link", linkSelection, parent).get
      result.asJson { case (200, json) => if ((json \ "ok").as[Boolean]) Right(true) else Left("") } must beRight(true).await
    }

    "return error if link not found" in {
      val parent: RESTRequest = mock[RESTRequest]
        .get returns new RESTResponse(Future.successful(Right(200, Json.obj("ok" -> false))))
      val service: PlayHATEOASClient = mock[PlayHATEOASClient]
        .executeGet(url) returns Future.successful(Right(200, Json.obj("ok" -> true)))
      val linkSelection: LinkSelection = mock[LinkSelection]
        .select(any) returns (Right(Links(Link(rel = "link", path = url))))

      val result = new ChainedCallRequest(service, "not_found", linkSelection, parent).get
      result.asJson { case (200, json) => if ((json \ "ok").as[Boolean]) Right(true) else Left("") } must beLeft("error: no link with rel = not_found found").await
    }
  }

}