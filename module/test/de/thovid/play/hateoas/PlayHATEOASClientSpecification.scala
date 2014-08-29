package de.thovid.play.hateoas

import org.specs2.mutable.Specification

import com.ning.http.client.Realm

class PlayHATEOASClientSpecification extends Specification {

  "play rest service configuration" should {
    "allow authentication" in {
      val service = new implementation.PlayHATEOASClient().withAuth("testname", "testpassword", Realm.AuthScheme.BASIC)
      service.auth must beSome("testname", "testpassword", Realm.AuthScheme.BASIC)
    }

    "allow custom headers" in {
      val service = new implementation.PlayHATEOASClient().withHeaders("X-test" -> "123", "X-other" -> "abv")
      service.headers must contain("X-test" -> "123")
      service.headers must contain("X-other" -> "abv")
    }
  }

}