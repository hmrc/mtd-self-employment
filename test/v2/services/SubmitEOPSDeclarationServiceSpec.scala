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

package v2.services

import java.time.LocalDate

import v2.mocks.connectors.MockDesConnector
import v2.models.errors._

import scala.concurrent.Future

class SubmitEOPSDeclarationServiceSpec extends ServiceSpec {

  class Test extends MockDesConnector {
    val service = new SubmitEOPSDeclarationService(
      desConnector = mockDesConnector
    )
  }

  val nino = "AA12356A"
  val from: LocalDate = LocalDate.parse("2017-01-01")
  val to: LocalDate = LocalDate.parse("2018-01-01")
  val selfEmploymentId = "test-se-id"


  "submitEOPSDeclaration" should {
    "return None" when {
      "the connector does not receive an error and returns None" in new Test {
        MockedDesConnector.submitEOPSDeclaration(nino, from, to, selfEmploymentId)
          .returns(Future.successful(None))

        val result: Option[ErrorResponse] = await(service.submitEOPSDeclaration(nino, from, to, selfEmploymentId))
        result shouldBe None
      }
    }

    Seq(
      "INVALID_IDTYPE" -> InternalError,
      "INVALID_IDVALUE" -> InvalidNinoFormatError,
      "INVALID_ACCOUNTINGPERIODSTARTDATE" -> InvalidPeriodStartDateError,
      "INVALID_ACCOUNTINGPERIODENDDATE" -> InvalidPeriodEndDateError,
      "INVALID_INCOMESOURCEID" -> NotFoundError,
      "INVALID_INCOMESOURCETYPE" -> InternalError,
      "CONFLICT" -> AlreadySubmittedError,
      "EARLY_SUBMISSION" -> EarlySubmissionError,
      "LATE_SUBMISSION" -> LateSubmissionError,
      "NOT_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> InternalError,
      "SERVICE_UNAVAILABLE" -> ServiceUnavailableError,
      "C55316" -> ConsolidatedExpensesError,
      "C55317" -> Class4Over16Error,
      "C55318" -> Class4PensionAgeError,
      "C55008" -> MismatchStartDateError,
      "C55013" -> MismatchEndDateError,
      "C55014" -> MismatchEndDateError
    ) foreach { case (desErrorCode, clientError) =>

        s"return the client error ${clientError.code}" when {
          s"the connector returns the des error code $desErrorCode" in new Test {
            val desError = SingleError(Error(desErrorCode, "some reason"))

            MockedDesConnector.submitEOPSDeclaration(nino, from, to, selfEmploymentId)
              .returns(Future.successful(Some(desError)))

            val result: Option[ErrorResponse] = await(service.submitEOPSDeclaration(nino, from, to, selfEmploymentId))
            val expectedErrorResponse = ErrorResponse(clientError, None)

            result shouldBe Some(expectedErrorResponse)
          }
        }
    }

    "return multiple client errors in the invalid request structure" when {
      "the connector returns multiple non-BVR errors" in new Test {
        val invalidNinoDesError = Error("INVALID_IDVALUE", "some reason")
        val invalidIncomeSourceIdDesError = Error("INVALID_INCOMESOURCEID", "some reason")

        val multipleDesErrors = MultipleErrors(Seq(invalidNinoDesError, invalidIncomeSourceIdDesError))

        MockedDesConnector.submitEOPSDeclaration(nino, from, to, selfEmploymentId)
          .returns(Future.successful(Some(multipleDesErrors)))

        val result: Option[ErrorResponse] = await(service.submitEOPSDeclaration(nino, from, to, selfEmploymentId))
        val expectedErrorResponse = ErrorResponse(InvalidRequestError, Some(Seq(InvalidNinoFormatError, NotFoundError)))

        result shouldBe Some(expectedErrorResponse)
      }
    }

    "return multiple client errors in the business error structure" when {
      "the connector returns multiple BVR errors" in new Test {
        val C55316Error = Error("C55316", "some reason")
        val C55317Error = Error("C55317", "some reason")

        val multipleBVRDesErrors = MultipleBVRErrors(Seq(C55316Error, C55317Error))

        MockedDesConnector.submitEOPSDeclaration(nino, from, to, selfEmploymentId)
          .returns(Future.successful(Some(multipleBVRDesErrors)))

        val result: Option[ErrorResponse] = await(service.submitEOPSDeclaration(nino, from, to, selfEmploymentId))
        val expectedErrorResponse = ErrorResponse(BusinessError, Some(Seq(ConsolidatedExpensesError, Class4Over16Error)))

        result shouldBe Some(expectedErrorResponse)
      }
    }
  }
}
