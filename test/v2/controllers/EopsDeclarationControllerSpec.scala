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

import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.MockIdGenerator
import v2.mocks.requestParsers.MockEopsDeclarationRequestDataParser
import v2.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockEopsDeclarationService, MockMtdIdLookupService}
import v2.models.EopsDeclarationSubmission
import v2.models.audit.AuditEvent
import v2.models.domain.Nino
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.inbound.EopsDeclarationRequestData
import v2.models.outcomes.DesResponse

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class EopsDeclarationControllerSpec extends ControllerBaseSpec {

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"
  val to: String = "2018-12-31"
  val selfEmploymentId: String = "X1IS12345678901"
  val correlationId: String = "x1234id"

  trait Test extends MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockEopsDeclarationService
    with MockEopsDeclarationRequestDataParser
    with TableDrivenPropertyChecks
    with MockAuditService
    with MockIdGenerator {

    val hc: HeaderCarrier = HeaderCarrier()

    lazy val target = new EopsDeclarationController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      eopsDeclarationService = mockEopsDeclarationService,
      requestDataParser = mockRequestDataParser,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAuditService.auditEventSucceeds(AuditEvent("submitEndOfPeriodStatement", "self-employment-submit-eops", "some details"))
    MockIdGenerator.generateCorrelationId.returns(correlationId)
  }

  private val requestJson =
    """
      |{
      |"finalised": true
      |}
    """.stripMargin

  "submit" should {
    "return a 204 (NO_CONTENT) response" when {
      "when valid data is supplied" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRequestData =
          EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(Json.parse(requestJson)))
        val eopsDeclarationSubmission: EopsDeclarationSubmission =
          EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))
        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Right(DesResponse(correlationId, ()))))

        val result: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
        status(result) shouldBe NO_CONTENT


      }
    }

    "return a 400 (BAD_REQUEST) with a single error" when {

      val badRequestErrors = List(
        InvalidStartDateError,
        InvalidEndDateError,
        InvalidRangeError,
        BadRequestError,
        NinoFormatError,
        SelfEmploymentIdError
      )

      for (error <- badRequestErrors) {
        eopsErrorStatusTester(error, BAD_REQUEST)
      }

    }

    "return a 400 (BAD_REQUEST) with multiple errors" when {

      "when a BadRequestError is generated" in new Test {
        val badRequestErrorContainer: ErrorWrapper = ErrorWrapper(
          correlationId,
          BadRequestError,
          Some(Seq(
            MissingStartDateError,
            InvalidEndDateError,
            NinoFormatError
          ))
        )

        val eopsDeclarationRequestData: EopsDeclarationRequestData =
          EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(Json.parse(requestJson)))
        val eopsDeclarationSubmission: EopsDeclarationSubmission =
          EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))

        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Left(badRequestErrorContainer)))

        val response: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
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
        BVRError,
        EarlySubmissionError,
        LateSubmissionError,
        NonMatchingPeriodError
      )

      for (error <- forbiddenErrors) {
        eopsErrorStatusTester(error, FORBIDDEN)
      }

      "a single error when a single BVRError occurs" in new Test {

        private val error = ConflictError

        val eopsDeclarationRequestData: EopsDeclarationRequestData =
          EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(Json.parse(requestJson)))
        val eopsDeclarationSubmission: EopsDeclarationSubmission =
          EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))

        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, error, None))))

        val result: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
        status(result) shouldBe FORBIDDEN
        (contentAsJson(result) \ "code").as[String] shouldBe error.code
        (contentAsJson(result) \ "message").as[String] shouldBe error.message
      }

    }

    "return a 403 (FORBIDDEN) with multiple errors" when {

      "when multiple BVR errors are generated" in new Test {
        val bvrErrorsContainer: ErrorWrapper = ErrorWrapper(
          correlationId,
          BVRError,
          Some(Seq(
            RuleClass4Over16,
            RuleClass4PensionAge,
            RuleMismatchStartDate,
            RuleMismatchEndDate,
            RuleConsolidatedExpenses
          ))
        )

        val eopsDeclarationRequestData: EopsDeclarationRequestData =
          EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(Json.parse(requestJson)))
        val eopsDeclarationSubmission: EopsDeclarationSubmission =
          EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))

        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Left(bvrErrorsContainer)))

        val response: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
        status(response) shouldBe FORBIDDEN
        contentAsJson(response) shouldBe Json.toJson(bvrErrorsContainer)
        (contentAsJson(response) \ "errors").as[JsArray].value.size shouldBe 5
      }

    }

    "return a 404 (NOT_FOUND) response" when {

      "when a NotFoundError is generated" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRequestData =
          EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(Json.parse(requestJson)))
        val eopsDeclarationSubmission: EopsDeclarationSubmission =
          EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))
        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, NotFoundError, None))))

        val result: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
        status(result) shouldBe NOT_FOUND

      }
    }

    "return a 500 (INTERNAL_SERVER_ERROR) response" when {

      "when a DownstreamError is generated" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRequestData =
          EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(Json.parse(requestJson)))
        val eopsDeclarationSubmission: EopsDeclarationSubmission =
          EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))
        MockedEopsDeclarationService.submit(eopsDeclarationSubmission)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, DownstreamError, None))))

        val result: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }

  def eopsErrorStatusTester(error: MtdError, expectedStatus: Int): Unit = {
    s"when a ${error.code} error occurs" in new Test {
      val eopsDeclarationRequestData: EopsDeclarationRequestData =
        EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(Json.parse(requestJson)))

      MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
        .returns(Left(ErrorWrapper(correlationId, error, None)))

      val response: Future[Result] = target.submit(nino, selfEmploymentId, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response).nonEmpty shouldBe true
    }
  }

}