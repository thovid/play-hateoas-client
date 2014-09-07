import scala.concurrent._
import com.ning.http.client.Realm.AuthScheme
import de.thovid.play.hateoas._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.WS.WSRequestHolder
import de.thovid.play.hateoas.linkformat.LinkFormat
import de.thovid.play.hateoas.linkformat.spring.SpringLinkFormat
import com.fasterxml.jackson.core.JsonParseException
import java.net.ConnectException

package de.thovid.play.hateoas {

  object HATEOAS {
    def client: HATEOASClient = client(SpringLinkFormat)
    def client(linkFormat: LinkFormat): HATEOASClient = new implementation.PlayHATEOASClient(linkFormat)
  }

  trait HATEOASClient {
    def withAuth(userName: String, password: String, scheme: AuthScheme): HATEOASClient
    def withHeaders(hdrs: (String, String)*): HATEOASClient
    def at(url: String): RESTRequest
  }

  trait RESTRequest {
    def get()(implicit executor: ExecutionContext): RESTResponse
    def delete()(implicit executor: ExecutionContext): RESTResponse
    def post(body: JsValue)(implicit executor: ExecutionContext): RESTResponse
    def put(body: JsValue)(implicit executor: ExecutionContext): RESTResponse
    def following(rel: String, selection: LinkSelection): RESTRequest
    def following(rel: String): RESTRequest = following(rel, fromToplevel)
  }

  trait LinkSelection {
    def select(json: JsValue, linkFormat: LinkFormat): Either[String, Links]
  }

  class RESTResponse(result: Future[Either[String, (Int, JsValue)]]) {

    def asJson[A](onSuccess: PartialFunction[(Int, JsValue), Either[String, A]])(implicit executor: ExecutionContext): Future[Either[String, A]] =
      result map { r =>
        r.right.flatMap { statusAndJson =>
          if (onSuccess.isDefinedAt(statusAndJson)) onSuccess(statusAndJson)
          else Left(s"error: could not handle result $r")
        }
      }
  }

  package implementation {

    class PlayHATEOASClient(val auth: Option[(String, String, AuthScheme)],
      val headers: Seq[(String, String)], val linkFormat: LinkFormat) extends HATEOASClient {
      def this(linkFormat: LinkFormat) = this(None, List(), linkFormat)

      def withAuth(userName: String, password: String, scheme: AuthScheme): PlayHATEOASClient =
        new PlayHATEOASClient(Some(userName, password, scheme), headers, linkFormat)

      def withHeaders(hdrs: (String, String)*): PlayHATEOASClient = new PlayHATEOASClient(auth, headers ++ hdrs, linkFormat)

      def at(url: String): RESTRequest = new SingleCallRequest(this, url)

      private[implementation] def executeGet(url: String)(implicit executor: ExecutionContext): Future[Either[String, (Int, JsValue)]] =
        execute(url, _.get, "GET")

      private[implementation] def executeDelete(url: String)(implicit executor: ExecutionContext): Future[Either[String, (Int, JsValue)]] =
        execute(url, _.delete, "DELETE")

      private[implementation] def executePost(url: String,
        body: JsValue)(implicit executor: ExecutionContext): Future[Either[String, (Int, JsValue)]] = execute(url, _.post(body), "POST")

      private[implementation] def executePut(url: String,
        body: JsValue)(implicit executor: ExecutionContext): Future[Either[String, (Int, JsValue)]] = execute(url, _.put(body), "PUT")

      private def execute(url: String,
        method: WSRequestHolder => Future[Response],
        methodName: String)(implicit executor: ExecutionContext): Future[Either[String, (Int, JsValue)]] = {
        Logger.debug(s"calling $methodName $url")
        method(serviceCall(url)) map (r => {
          val result = json(r).right.map(j => (r.status, j))
          Logger.debug(s"received response ${r.status}: ${r.body}")
          result
        }) recover {
          case e: ConnectException => Left(s"error: could not connect to $url")
        }
      }

      private def serviceCall(url: String) = addAuth(WS.url(url)).withHeaders(headers: _*)

      private def addAuth(requestHolder: WSRequestHolder) =
        auth map { a => requestHolder.withAuth(a._1, a._2, a._3) } getOrElse (requestHolder)

      private def asError(method: String, path: String, response: Response) =
        Left(s"error: $method $path returned ${response.status} - ${response.statusText}")

      private def json(response: Response): Either[String, JsValue] =
        if (response.body.isEmpty) Right(Json.obj())
        else try { Right(response.json) } catch {
          case e: JsonParseException => Left("error: body not json")
        }
    }

    private[implementation] abstract class AbstractRESTRequest(service: PlayHATEOASClient) extends RESTRequest {
      def following(rel: String, selection: LinkSelection): RESTRequest = new ChainedCallRequest(service, rel, selection, this)
    }

    private[implementation] class SingleCallRequest(service: PlayHATEOASClient, url: String) extends AbstractRESTRequest(service) {
      def get()(implicit executor: ExecutionContext): RESTResponse = new RESTResponse(service.executeGet(url))

      def post(body: JsValue)(implicit executor: ExecutionContext): RESTResponse = new RESTResponse(service.executePost(url, body))

      def put(body: JsValue)(implicit executor: ExecutionContext): RESTResponse = new RESTResponse(service.executePut(url, body))

      def delete()(implicit executor: ExecutionContext): RESTResponse = new RESTResponse(service.executeDelete(url))
    }

    private[implementation] class ChainedCallRequest(service: PlayHATEOASClient,
      rel: String,
      linkSelection: LinkSelection,
      parent: RESTRequest) extends AbstractRESTRequest(service) {

      def get()(implicit executor: ExecutionContext): RESTResponse = call(link => service.executeGet(link.path))

      def delete()(implicit executor: ExecutionContext): RESTResponse = call(link => service.executeDelete(link.path))

      def post(body: JsValue)(implicit executor: ExecutionContext): RESTResponse = call(link => service.executePost(link.path, body))

      def put(body: JsValue)(implicit executor: ExecutionContext): RESTResponse = call(link => service.executePut(link.path, body))

      private def call(method: Link => Future[Either[String, (Int, JsValue)]])(implicit executor: ExecutionContext): RESTResponse = {
        val result = parent.get.asJson {
          case (200, json) => linkSelection.select(json, service.linkFormat)
        }.flatMap {
          _.fold(
            error => Future.successful(Left(error)),
            links => links.get(rel) map (method)
              getOrElse (Future.successful(Left(s"error: no link with rel = $rel found"))))
        }
        new RESTResponse(result)
      }
    }
  }
}