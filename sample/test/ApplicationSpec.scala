import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import de.thovid.play.hateoas._
import de.thovid.play.hateoas.linkformat.spring.SpringLinkFormat._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "HATEOAS Client" should {

    "get some json reponse" in new WithServer {
      val result = HATEOAS.client
        .at(s"http://localhost:$port/samples/1")
        .get()
        .asJson {
          case (OK, json) => name(json)
        }
      result must beRight("First Sample").await
    }

    "follow a link selected from a list of entries" in new WithServer {
      val result = HATEOAS.client
        .at(s"http://localhost:$port/samples")
        .following("self", selectedBy("samples" -> "id", "2"))
        .get()
        .asJson {
          case (OK, json) => name(json)
        }

      result must beRight("Second Sample").await
    }
  }

  private def name(json: JsValue): Either[String, String] = (json \ "name").asOpt[String] map (n => Right(n)) getOrElse (Left("nok"))
}
