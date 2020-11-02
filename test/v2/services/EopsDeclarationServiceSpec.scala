/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.domain.Nino
import v2.mocks.connectors.MockDesConnector
import v2.mocks.services.MockAuditService
import v2.models.EopsDeclarationSubmission
import v2.models.auth.UserDetails
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.outcomes.{DesResponse, EopsDeclarationOutcome}

import scala.concurrent.Future

class EopsDeclarationServiceSpec extends ServiceSpec {

  private trait Test extends MockAuditService with MockDesConnector {
    implicit val userDetails: UserDetails = UserDetails("1234567890", "Individual", None)

    val nino: Nino = Nino("AA123567A")
    val from: LocalDate = LocalDate.parse("2017-01-01")
    val to: LocalDate = LocalDate.parse("2018-01-01")
    val selfEmploymentId: String = "test-se-id"

    val service = new EopsDeclarationService(mockDesConnector)
  }

  "calling submit with valid arguments" should {

    "return None without errors " when {
      "successful request is made end des" in new Test {

        MockDesConnector.submitEOPSDeclaration(nino.nino, from, to, selfEmploymentId)
          .returns(Future.successful(Right(DesResponse(correlationId, ()))))

        private val result = await(service.submit(
          EopsDeclarationSubmission(nino, selfEmploymentId, from, to)))

        result shouldBe Right(DesResponse(correlationId, ()))
      }
    }

    "return multiple errors " when {
      "des connector returns sequence of errors" in new Test {

        val desResponse: MultipleErrors = MultipleErrors(Seq(MtdError("INVALID_ACCOUNTINGPERIODENDDATE", "some reason"),
          MtdError("INVALID_ACCOUNTINGPERIODSTARTDATE", "some reason")))

        MockDesConnector.submitEOPSDeclaration(nino.nino, from, to, selfEmploymentId)
          .returns(Future.successful(Left(DesResponse(correlationId, desResponse))))

        val expected: ErrorWrapper = ErrorWrapper(correlationId, BadRequestError, Some(Seq(InvalidEndDateError, InvalidStartDateError)))

        private val result = await(service.submit(
          EopsDeclarationSubmission(nino, selfEmploymentId, from, to)))


        result shouldBe Left(expected)
      }
    }

    "return multiple bvr errors " when {
      "des connector returns sequence of bvr errors" in new Test {

        val desResponse: BVRErrors = BVRErrors(Seq(MtdError("C55317", "some reason"),
          MtdError("C55318", "some reason")))

        MockDesConnector.submitEOPSDeclaration(nino.nino, from, to, selfEmploymentId)
          .returns(Future.successful(Left(DesResponse(correlationId, desResponse))))

        val expected: ErrorWrapper = ErrorWrapper(correlationId, BVRError, Some(Seq(RuleClass4Over16, RuleClass4PensionAge)))

        private val result = await(service.submit(
          EopsDeclarationSubmission(nino, selfEmploymentId, from, to)))

        result shouldBe Left(expected)
      }
    }

    "return single bvr error " when {
      "des connector returns single of bvr error" in new Test {

        val desResponse: BVRErrors = BVRErrors(Seq(MtdError("C55317", "some reason")))

        MockDesConnector.submitEOPSDeclaration(nino.nino, from, to, selfEmploymentId)
          .returns(Future.successful(Left(DesResponse(correlationId, desResponse))))

        val expected: ErrorWrapper = ErrorWrapper(correlationId, RuleClass4Over16, None)

        private val result = await(service.submit(
          EopsDeclarationSubmission(nino, selfEmploymentId, from, to)))

        result shouldBe Left(expected)
      }
    }

    val possibleDesErrors: Seq[(String, String, MtdError)] = Seq(
      ("NOT_FOUND", "not found", NotFoundError),
      ("INVALID_IDTYPE", "downstream", DownstreamError),
      ("SERVICE_UNAVAILABLE", "service unavailable", ServiceUnavailableError),
      ("SERVER_ERROR", "downstream", DownstreamError),
      ("INVALID_IDVALUE", "invalid nino", NinoFormatError),
      ("INVALID_ACCOUNTINGPERIODENDDATE", "invalid end date", InvalidEndDateError),
      ("INVALID_ACCOUNTINGPERIODSTARTDATE", "invalid start date", InvalidStartDateError),
      ("CONFLICT", "duplicate submission", ConflictError),
      ("EARLY_SUBMISSION", "early submission", EarlySubmissionError),
      ("LATE_SUBMISSION", "late submission", LateSubmissionError),
      ("NON_MATCHING_PERIOD", "non matching period", NonMatchingPeriodError)
    )

    possibleDesErrors.foreach {
      case (desCode, description, mtdError) =>
        s"return a $description error" when {
          s"the DES connector returns a $desCode code" in new Test {

            val error: EopsDeclarationOutcome = Left(DesResponse(correlationId, SingleError(MtdError(desCode, ""))))

            MockDesConnector.submitEOPSDeclaration(nino.nino, from, to, selfEmploymentId)
              .returns(Future.successful(error))

            private val result = await(service.submit(
              EopsDeclarationSubmission(nino, selfEmploymentId, from, to)))

            result shouldBe Left(ErrorWrapper(correlationId, mtdError, None))
          }
        }
    }

  }

}