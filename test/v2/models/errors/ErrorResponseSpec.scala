/*
 * Copyright 2018 HM Revenue & Customs
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

package v2.models.errors

import play.api.libs.json.Json
import support.UnitSpec

class ErrorResponseSpec extends UnitSpec {

  "writes" should {
    "successfully write an ErrorResponse model to json without an array of errors" when {
      "the model contains a single error" in {
        val errorResponse = ErrorResponse(Error("TEST_CODE", "some message"), None)

        val expectedWriteResult = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "message": "some message"
            |}
          """.stripMargin)

        ErrorResponse.writes.writes(errorResponse) shouldBe expectedWriteResult
      }

      "the model contains an empty sequence of errors" in {
        val errorResponse = ErrorResponse(Error("TEST_CODE", "some message"), Some(Seq()))

        val expectedWriteResult = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "message": "some message"
            |}
          """.stripMargin)

        ErrorResponse.writes.writes(errorResponse) shouldBe expectedWriteResult
      }
    }

    "successfully write an ErrorResponse model to json with an array of errors" when {
      "the model contains a sequence of errors" in {
        val errorResponse = ErrorResponse(
          Error("TEST_CODE", "some message"),
          Some(Seq(
            Error("TEST_CODE_1", "some message"),
            Error("TEST_CODE_2", "some message")
          ))
        )

        val expectedWriteResult = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "message": "some message",
            |  "errors": [
            |    {
            |      "code": "TEST_CODE_1",
            |      "message": "some message"
            |    },
            |    {
            |      "code": "TEST_CODE_2",
            |      "message": "some message"
            |    }
            |  ]
            |}
          """.stripMargin)

        ErrorResponse.writes.writes(errorResponse) shouldBe expectedWriteResult
      }
    }
  }
}
