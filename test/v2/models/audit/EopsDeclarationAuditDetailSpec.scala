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

package v2.models.audit

import play.api.http.Status
import play.api.libs.json.Json
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class EopsDeclarationAuditDetailSpec extends UnitSpec with JsonErrorValidators {

  val nino: String = "MA123456D"
  val from: String = "2017-06-04"
  val to: String = "2018-06-04"
  private val responseSuccess = EopsDeclarationAuditResponse(Status.NO_CONTENT, None)
  private val responseFail = EopsDeclarationAuditResponse(Status.BAD_REQUEST, Some(Seq(AuditError("FORMAT_NINO"))))

  val request = Json.obj("finalised" -> true)

  private val eopsDeclarationAuditDetail =
    EopsDeclarationAuditDetail("Agent", Some("123456780"), nino, from, to, request, "5b85344c1100008e00c6a181", "XAIS12345678987", responseSuccess)

  val eopsDeclarationAuditDetailAgentJson: String =
    """
      |{
      | "userType": "Agent",
      | "agentReferenceNumber": "123456780",
      | "nino": "MA123456D",
      | "from": "2017-06-04",
      | "to": "2018-06-04",
      | "request": {
      |    "finalised": true
      |  },
      | "X-CorrelationId": "5b85344c1100008e00c6a181",
      | "incomeSourceId": "XAIS12345678987",
      | "response": {
      |    "httpStatus": 204
      |  }
      |}
    """.stripMargin

  val eopsDeclarationAuditDetailIndivualJson: String =
    """
      |{
      | "userType": "individual",
      | "nino": "MA123456D",
      | "from": "2017-06-04",
      | "to": "2018-06-04",
      | "request": {
      |    "finalised": true
      |  },
      | "X-CorrelationId": "5b85344c1100008e00c6a181",
      | "incomeSourceId": "XAIS12345678987",
      | "response": {
      |    "httpStatus": 400,
      |    "errors": [
      |      {
      |        "errorCode": "FORMAT_NINO"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin

  "EopsDeclarationAuditDetail writes" should {

    "return a valid json with all the fields" in {
      Json.toJson(eopsDeclarationAuditDetail) shouldBe Json.parse(eopsDeclarationAuditDetailAgentJson)
    }

    "return a valid json with only mandatory fields" in {
      Json.toJson(eopsDeclarationAuditDetail.copy(userType = "individual", agentReferenceNumber = None, response = responseFail)) shouldBe
        Json.parse(eopsDeclarationAuditDetailIndivualJson)
    }
  }
}
