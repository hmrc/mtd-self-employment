/*
 * Copyright 2019 HM Revenue & Customs
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

package v2.connectors.httpparsers

import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import v2.connectors.httpparsers.SubmitEOPSDeclarationHttpParser.submitEOPSDeclarationHttpReads
import v2.models.errors._
import v2.models.outcomes.DesResponse

class SubmitEopsDeclarationHttpParserSpec extends HttpParserSpec {

  val correlationId = "x1234id"

  "read" should {
    "return a None" when {
      "the http response contains a 204" in {

        val httpResponse = HttpResponse(NO_CONTENT, None, Map("CorrelationId" -> Seq(correlationId)))

        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Right(DesResponse(correlationId, ()))
      }
      "the http response contains a 202 (as a workaround for a DES inconsistency)" in {
        val correlationId = "x1234id"
        val httpResponse = HttpResponse(ACCEPTED, None, Map("CorrelationId" -> Seq(correlationId)))

        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Right(DesResponse(correlationId, ()))
      }
    }

    "return a single error" when {
      "the http response contains a 400 with an error response body" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "reason": "some reason"
            |}
          """.stripMargin)
        val expected = SingleError(MtdError("TEST_CODE", "some reason"))

        val httpResponse = HttpResponse(BAD_REQUEST, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }

      "the http response contains a 403 with an error response body" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "reason": "some reason"
            |}
          """.stripMargin)
        val expected = SingleError(MtdError("TEST_CODE", "some reason"))

        val httpResponse = HttpResponse(FORBIDDEN, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }

      "the http response contains a 409 with an error response body" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "reason": "some reason"
            |}
          """.stripMargin)
        val expected = SingleError(MtdError("TEST_CODE", "some reason"))

        val httpResponse = HttpResponse(CONFLICT, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }
    }

    "return a generic error" when {
      "the http response contains a 404 with an error response body" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "reason": "some reason"
            |}
          """.
            stripMargin)
        val expected = GenericError(NotFoundError)

        val httpResponse = HttpResponse(NOT_FOUND, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }

      "the http response contains a 500 with an error response body" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "reason": "some reason"
            |}
          """.
            stripMargin)
        val expected = GenericError(DownstreamError)

        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }

      "the http response contains a 503 with an error response body" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "reason": "some reason"
            |}
          """.
            stripMargin)
        val expected = GenericError(ServiceUnavailableError)

        val httpResponse = HttpResponse(SERVICE_UNAVAILABLE, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }
    }

    "return multiple errors" when {
      "the http response contains a 400 with an error response body with multiple errors" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "failures": [
            |    {
            |      "code": "TEST_CODE_1",
            |      "reason": "some reason"
            |    },
            |    {
            |      "code": "TEST_CODE_2",
            |      "reason": "some reason"
            |    }
            |  ]
            |}
          """.stripMargin)
        val expected = MultipleErrors(Seq(MtdError("TEST_CODE_1", "some reason"), MtdError("TEST_CODE_2", "some reason")))

        val httpResponse = HttpResponse(BAD_REQUEST, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }
    }

    "return bvr errors" when {
      "the http response contains a 403 with an error response body with bvr errors" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "bvrfailureResponseElement": {
            |    "validationRuleFailures": [
            |      {
            |        "id": "TEST_ID_1",
            |        "type": "err",
            |        "text": "some text"
            |      },
            |      {
            |        "id": "TEST_ID_2",
            |        "type": "err",
            |        "text": "some text"
            |      }
            |    ]
            |  }
            |}
          """.stripMargin)
        val expected = BVRErrors(Seq(MtdError("TEST_ID_1", ""), MtdError("TEST_ID_2", "")))

        val httpResponse = HttpResponse(FORBIDDEN, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }
    }
  }
}
