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

package v2.controllers

import java.time.LocalDate

import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.services.{MockEnrolmentsAuthService, MockEopsDeclarationService, MockMtdIdLookupService}
import v2.models.EopsDeclarationSubmission
import v2.models.errors.SubmitEopsDeclarationErrors.{InvalidEndDateError, _}
import v2.models.errors._

import scala.concurrent.Future

class EopsDeclarationControllerSpec extends ControllerBaseSpec {

  trait Test extends MockEnrolmentsAuthService with MockMtdIdLookupService with MockEopsDeclarationService with TableDrivenPropertyChecks {

    val hc = HeaderCarrier()

    lazy val target = new EopsDeclarationController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      eopsDeclarationService = mockEopsDeclarationService
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()

  }

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"
  val to: String = "2018-12-31"
  val selfEmploymentId = "ABCDEFGHIJKLMNO"

  "submit" should {
    "return a 204 (NO_CONTENT) response" when {
      "when valid data is supplied" in new Test {

        val eopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(None))

        val result = target.submit(nino, selfEmploymentId, from, to)(FakeRequest())
        status(result) shouldBe NO_CONTENT

      }
    }

    "return a 400 (BAD_REQUEST) with a single error" when {

      val badRequestErrors = List(
        InvalidStartDateError,
        InvalidEndDateError,
        InvalidRangeError,
        BadRequestError,
        InvalidNinoError,
        EarlySubmissionError,
        NinoFormatError,
        LateSubmissionError
      )

      for (error <- badRequestErrors) {
        eopsErrorStatusTester(error, BAD_REQUEST)
      }

    }

    "return a 400 (BAD_REQUEST) with multiple errors" when {

      "when a BadRequestError is generated" in new Test {
        val badRequestErrorContainer = ErrorResponse(BadRequestError, Some(Seq(MissingStartDateError,
          InvalidEndDateError,
          NinoFormatError))
        )

        val eopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))
        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Some(badRequestErrorContainer)))

        val response: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(FakeRequest())
        status(response) shouldBe BAD_REQUEST
        contentAsJson(response) shouldBe Json.toJson(badRequestErrorContainer)
        (contentAsJson(response) \ "errors").as[JsArray].value.size shouldBe 3
      }

    }

    "return a 403 (FORBIDDEN) with a single error" when {

      val forbiddenErrors = List(
        ConflictError,
        NotFinalisedDeclaration,
        RuleClass4Over16,
        RuleClass4PensionAge,
        RuleMismatchStartDate,
        RuleMismatchEndDate,
        RuleConsolidatedExpenses,
        BVRError
      )

      for (error <- forbiddenErrors) {
        eopsErrorStatusTester(error, FORBIDDEN)
      }

      "a single error when a single BVRError occurs" in new Test {

        val error = ConflictError

        val eopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Some(ErrorResponse(error, None))))

        val result = target.submit(nino, selfEmploymentId, from, to)(FakeRequest())
        status(result) shouldBe FORBIDDEN
        (contentAsJson(result) \ "code").as[String] shouldBe error.code
        (contentAsJson(result) \ "message").as[String] shouldBe error.message
      }

    }

    "return a 403 (FORBIDDEN) with multiple errors" when {

      "when multiple BVR errors are generated" in new Test {
        val bvrErrorsContainer = ErrorResponse(BVRError, Some(Seq(
          RuleClass4Over16,
          RuleClass4PensionAge,
          RuleMismatchStartDate,
          RuleMismatchEndDate,
          RuleConsolidatedExpenses
        )))

        val eopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))
        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Some(bvrErrorsContainer)))

        val response: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(FakeRequest())
        status(response) shouldBe FORBIDDEN
        contentAsJson(response) shouldBe Json.toJson(bvrErrorsContainer)
        (contentAsJson(response) \ "errors").as[JsArray].value.size shouldBe 5
      }

    }

    "return a 404 (NOT_FOUND) response" when {

      "when a NotFoundError is generated" in new Test {

        val eopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Some(ErrorResponse(NotFoundError, None))))

        val result = target.submit(nino, selfEmploymentId, from, to)(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }
    }

    "return a 500 (INTERNAL_SERVER_ERROR) response" when {

      "when a DownstreamError is generated" in new Test {

        val eopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Some(ErrorResponse(DownstreamError, None))))

        val result = target.submit(nino, selfEmploymentId, from, to)(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }

  def eopsErrorStatusTester(error: v2.models.errors.Error, expectedStatus: Int): Unit = {
    s"when a ${error.code} error occurs" in new Test {
      val eopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))
      MockedEopsDeclarationService.submit(eopsDeclarationSubmission).returns(Future.successful(Some(ErrorResponse(error, None))))
      val response: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(FakeRequest())
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
    }
  }

}
