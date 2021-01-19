/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import v2.connectors.httpparsers.SubmitEOPSDeclarationHttpParser.submitEOPSDeclarationHttpReads
import v2.models.errors._
import v2.models.outcomes.{DesResponse, EopsDeclarationOutcome}

class SubmitEopsDeclarationHttpParserSpec extends HttpParserSpec {

  val correlationId: String = "x1234id"

  "read" should {
    "return a None" when {
      "the http response contains a 204" in {

        val httpResponse: HttpResponse = HttpResponse(NO_CONTENT, body = "", headers = Map("CorrelationId" -> Seq(correlationId)))

        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Right(DesResponse(correlationId, ()))
      }
      "the http response contains a 202 (as a workaround for a DES inconsistency)" in {
        val correlationId: String = "x1234id"
        val httpResponse: HttpResponse = HttpResponse(ACCEPTED, body = "", headers = Map("CorrelationId" -> Seq(correlationId)))

        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
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
        val expected: SingleError = SingleError(MtdError("TEST_CODE", "some reason"))

        val httpResponse: HttpResponse = HttpResponse(BAD_REQUEST, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
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
        val expected: SingleError = SingleError(MtdError("TEST_CODE", "some reason"))

        val httpResponse: HttpResponse = HttpResponse(FORBIDDEN, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
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
        val expected: SingleError = SingleError(MtdError("TEST_CODE", "some reason"))

        val httpResponse: HttpResponse = HttpResponse(CONFLICT, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }
    }

    "return a generic error" when {
      "the http response contains a 404 with an error response body" in {
        val errorResponseJson: JsValue = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "reason": "some reason"
            |}
          """.
            stripMargin)
        val expected: GenericError = GenericError(NotFoundError)

        val httpResponse: HttpResponse = HttpResponse(NOT_FOUND, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
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
        val expected: GenericError = GenericError(DownstreamError)

        val httpResponse: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
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
        val expected: GenericError = GenericError(ServiceUnavailableError)

        val httpResponse: HttpResponse = HttpResponse(SERVICE_UNAVAILABLE, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }
    }

    "return multiple errors" when {
      "the http response contains a 400 with an error response body with multiple errors" in {
        val errorResponseJson: JsValue = Json.parse(
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
        val expected: MultipleErrors = MultipleErrors(Seq(MtdError("TEST_CODE_1", "some reason"), MtdError("TEST_CODE_2", "some reason")))

        val httpResponse: HttpResponse = HttpResponse(BAD_REQUEST, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }
    }

    "return bvr errors" when {
      "the http response contains a 403 with an error response body with bvr errors" in {
        val errorResponseJson: JsValue = Json.parse(
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
        val expected: BVRErrors = BVRErrors(Seq(MtdError("TEST_ID_1", ""), MtdError("TEST_ID_2", "")))

        val httpResponse: HttpResponse = HttpResponse(FORBIDDEN, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
        val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
        result shouldBe Left(DesResponse(correlationId, expected))
      }
    }

    "return an outbound error if the error JSON doesn't match the Error model" in {
      val errorResponseJson = Json.parse(
        """
          |{
          |  "this": "TEST_CODE",
          |  "that": "some reason"
          |}
        """.stripMargin)
      val expected: DesResponse[GenericError] = DesResponse(correlationId, GenericError(DownstreamError))

      val httpResponse: HttpResponse = HttpResponse(CONFLICT, errorResponseJson.toString(), Map("CorrelationId" -> Seq(correlationId)))
      val result: EopsDeclarationOutcome = submitEOPSDeclarationHttpReads.read(POST, "/test", httpResponse)
      result shouldBe Left(expected)
    }
  }
}