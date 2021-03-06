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

package v2.controllers

import java.time.LocalDate

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.controllers.requestParsers.EopsDeclarationRequestDataParser
import v2.mocks.validators.MockEopsDeclarationRequestDataValidator
import v2.models.EopsDeclarationSubmission
import v2.models.errors.{BadRequestError, ErrorWrapper, InvalidStartDateError, NinoFormatError}
import v2.models.inbound.EopsDeclarationRequestData

class EopsDeclarationRequestDataParserSpec extends UnitSpec {

  val validNino: String = "AA123456A"
  val validSelfEmploymentId: String = "X1AAAAAAAAAAAA5"
  val validFromDate: String = "2018-01-01"
  val validToDate: String = "2018-12-31"
  val validJsonBody: AnyContentAsJson = AnyContentAsJson(Json.obj("finalised" -> true))
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  trait Test extends MockEopsDeclarationRequestDataValidator {
    lazy val parser = new EopsDeclarationRequestDataParser(mockValidator)
  }


  "parseRequest" should {

    "return an EopsDeclaration submission" when {
      "valid request data is supplied" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRequestData =
        EopsDeclarationRequestData(validNino, validSelfEmploymentId, validFromDate, validToDate, validJsonBody)

        val eopsDeclarationSubmissionRequest: EopsDeclarationSubmission =
        EopsDeclarationSubmission(Nino(validNino), validSelfEmploymentId, LocalDate.parse(validFromDate), LocalDate.parse(validToDate))


        MockedEopsDeclarationInputDataValidator.validate(eopsDeclarationRequestData).returns(List())

        parser.parseRequest(eopsDeclarationRequestData) shouldBe Right(eopsDeclarationSubmissionRequest)
      }
    }

    "return an ErrorWrapper" when {

      val invalidNino: String = "foobar"
      val invalidFromDate: String = "bad-date"

      "a single validation error occurs" in new Test {
        val eopsDeclarationRequestData: EopsDeclarationRequestData =
          EopsDeclarationRequestData(invalidNino, validSelfEmploymentId, validFromDate, validToDate, validJsonBody)

        val singleErrorWrapper: ErrorWrapper =
          ErrorWrapper(correlationId, NinoFormatError, None)

        MockedEopsDeclarationInputDataValidator.validate(eopsDeclarationRequestData)
          .returns(List(NinoFormatError))

        parser.parseRequest(eopsDeclarationRequestData) shouldBe Left(singleErrorWrapper)
      }

      "multiple validation errors occur" in new Test {
        val eopsDeclarationRequestData: EopsDeclarationRequestData =
          EopsDeclarationRequestData(invalidNino, validSelfEmploymentId, invalidFromDate, validToDate, validJsonBody)

        val multipleErrorWrapper: ErrorWrapper =
          ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, InvalidStartDateError)))

        MockedEopsDeclarationInputDataValidator.validate(eopsDeclarationRequestData)
          .returns(List(NinoFormatError, InvalidStartDateError))

        parser.parseRequest(eopsDeclarationRequestData) shouldBe Left(multipleErrorWrapper)
      }
    }

  }
}