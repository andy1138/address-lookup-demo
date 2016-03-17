/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers


import play.api.mvc.Results._
import services.AddressLookupWS

import concurrent._
import play.api.mvc._
import play.api.test._
import org.scalatestplus.play._


import play.api.test.Helpers._

class AddressLookupSpec extends PlaySpec with Results with OneAppPerSuite {

  //  trait AddressLookupTestController extends AddressLookupController with Controller


  "addressLookup action" should {

    "check address template" in {
      // new WithApplication
      val html = views.html.addresslookup.address_lookup(AddressTypedDetails(""), None, Countries.countries, None, None)(FakeRequest())
      contentAsString(html) must include("Your Address")
    }

    "return a blank page" in {
      val controller = new AddressLookupController with DummyWS
      val request = FakeRequest()
      val result = controller.addressLookup().apply(request)

      val bodyText: String = contentAsString(result)
      bodyText must include("Your Address")
    }
  }

  "addressLookupSelection action" should {
    "check if 'edit this addr' is selected we return edit fields" in {
      val controller = new AddressLookupController with DataWS
      val request = FakeRequest(GET,
        "/address-lookup-demo/address-lookup-selection?hiddenselection=hiddenselection&radio-inline-group=GB00001&UK-postcode=AA1AA1"
      )
      val result = controller.addressLookupSelection().apply(request)

      val bodyText: String = contentAsString(result)
      bodyText must include("Address line 1")
      bodyText must include("Address line 2")
      bodyText must include("Nearest town or city")
      bodyText must include("County")
    }

    "check if 'continue' is selected return a list of matching addresses" in {
      val controller = new AddressLookupController with DataWS
      val request = FakeRequest(GET, "/address-lookup-demo/address-lookup-selection?UK-postcode=AA1AA1")
      val result = controller.addressLookupSelection().apply(request)

      val bodyText: String = contentAsString(result)
      bodyText must include("AA1AA1")
    }

    "A postcode with >50 addresses will display an error message" in {
      val controller = new AddressLookupController with Data60ItemsWS
      val request = FakeRequest(GET, "/address-lookup-demo/address-lookup-selection?UK-postcode=AA1AA1")
      val result = controller.addressLookupSelection().apply(request)

      val bodyText: String = contentAsString(result)
      bodyText must include("Over 50 addresses found")
    }

    "A postcode with 'random' data will display an error message" in {
      val controller = new AddressLookupController with DummyBadRequestWS
      val request = FakeRequest(GET, "/address-lookup-demo/address-lookup-selection?house-name-number=&UK-postcode=nfjewk")
      val result = controller.addressLookupSelection().apply(request)

      val bodyText: String = contentAsString(result)
      bodyText must include("The postcode was unrecognised")
    }

    "check if 'continue' is selected we return edit fields" in {
      val controller = new AddressLookupController with DummyWS
      val request = FakeRequest(GET, "/address-lookup-demo/address-lookup-selection?UK-postcode=AA1AA1")
      val result = controller.addressLookupSelection().apply(request)

      val bodyText: String = contentAsString(result)
      bodyText mustNot include("Address line 1")
    }

    "display warning if no postcode entered" in {
      val controller = new AddressLookupController with DummyWS
      val request = FakeRequest(GET, "/address-lookup-demo/address-lookup-selection")
      val result = controller.addressLookupSelection().apply(request)

      val bodyText: String = contentAsString(result)
      bodyText mustNot include("Post code")
    }
  }

  "completion page is displayed" should {
    "we press continue on the list of addrs" in {
      val controller = new AddressLookupController with DummyWS
      val request = FakeRequest(GET,
        "http://localhost:9000/address-lookup-demo/address-lookup-selection?house-name-number=&UK-postcode=AA1AA1&radio-inline-group=GB00001"
      )
      val result: Future[Result] = controller.addressLookupSelection().apply(request)

      val bodyText: String = contentAsString(result)
      bodyText must include("Application complete")
    }
  }
}


trait DummyWS extends AddressLookupWS {
  def findAddresses(postcode: String, filter: Option[String]): Future[Either[Status, Option[List[services.Address]]]] = {
    Future.successful(Right(Some(List[services.Address]())))
  }
}

trait DataWS extends AddressLookupWS {
  def findAddresses(postcode: String, filter: Option[String]): Future[Either[Status, Option[List[services.Address]]]] = {
    Future.successful(Right(Some(List[services.Address](
      services.Address("GB00001", Array[String](""), "ATown", "AA1AA1")
    ))))
  }
}

trait Data60ItemsWS extends AddressLookupWS {
  val ListSize = 60

  def findAddresses(postcode: String, filter: Option[String]): Future[Either[Status, Option[List[services.Address]]]] = {
    Future.successful(Right(Some(List.fill[services.Address](ListSize) {
      services.Address("GB00001", Array[String](""), "ATown", "AA1AA1")
    })))
  }
}

trait DummyBadRequestWS extends AddressLookupWS {
  def findAddresses(postcode: String, filter: Option[String]): Future[Either[Status, Option[List[services.Address]]]] = {
    Future.successful(Left(BadRequest))
  }
}
