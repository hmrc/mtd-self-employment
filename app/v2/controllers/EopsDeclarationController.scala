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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v2.controllers.requestParsers.EopsDeclarationRequestDataParser
import v2.models.audit.{AuditError, AuditEvent, EopsDeclarationAuditDetail, EopsDeclarationAuditResponse}
import v2.models.auth.UserDetails
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.inbound.EopsDeclarationRequestData
import v2.services.{AuditService, EnrolmentsAuthService, EopsDeclarationService, MtdIdLookupService}
import v2.utils.{IdGenerator, Logging}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EopsDeclarationController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          eopsDeclarationService: EopsDeclarationService,
                                          requestDataParser: EopsDeclarationRequestDataParser,
                                          auditService: AuditService,
                                          cc: ControllerComponents,
                                          val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(
      controllerName = "EopsDeclarationController",
      endpointName = "submitEndOfPeriodStatement"
    )

  def submit(nino: String, selfEmploymentId: String, from: String, to: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
          s"with CorrelationId: $correlationId")

      implicit val userDetails: UserDetails = request.userDetails

      requestDataParser.parseRequest(EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(request.body))) match {
        case Right(eopsDeclarationSubmission) =>
          eopsDeclarationService.submit(eopsDeclarationSubmission).map {
            case Right(desResponse) =>
              logger.info(s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}]" +
                s" - Success response received with CorrelationId: ${desResponse.correlationId}")
              auditEopsSubmission(nino, selfEmploymentId, from, to, request.request.body,
                desResponse.correlationId, userDetails, EopsDeclarationAuditResponse(NO_CONTENT, None))
              NoContent.withHeaders("X-CorrelationId" -> desResponse.correlationId)
            case Left(errorResponse) =>
              val resCorrelationId = errorResponse.correlationId
              val result = processError(errorResponse).withHeaders("X-CorrelationId" -> resCorrelationId)
              logger.info(
                s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
                  s"Error response received with CorrelationId: $resCorrelationId")
              auditEopsSubmission(nino, selfEmploymentId, from, to, request.request.body, resCorrelationId,
                userDetails, EopsDeclarationAuditResponse(result.header.status, Some(errorResponse.allErrors.map(error => AuditError(error.code)))))
              result
          }
        case Left(validationErrorResponse) =>
          val resCorrelationId = validationErrorResponse.correlationId
          val result = processError(validationErrorResponse).withHeaders("X-CorrelationId" -> resCorrelationId)
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Error response received with CorrelationId: $resCorrelationId")
          auditEopsSubmission(nino, selfEmploymentId, from, to, request.request.body, resCorrelationId,
            userDetails, EopsDeclarationAuditResponse(result.header.status, Some(validationErrorResponse.allErrors.map(error => AuditError(error.code)))))
          Future.successful(result)
      }
    }

  private def processError(errorResponse: ErrorWrapper): Result = {
    (errorResponse.error: @unchecked) match {
      case InvalidStartDateError
           | InvalidEndDateError
           | InvalidRangeError
           | BadRequestError
           | NinoFormatError
           | SelfEmploymentIdError =>
        BadRequest(Json.toJson(errorResponse))
      case ConflictError
           | EarlySubmissionError
           | LateSubmissionError
           | NotFinalisedDeclaration
           | RuleClass4Over16
           | RuleClass4PensionAge
           | RuleMismatchStartDate
           | RuleMismatchEndDate
           | RuleConsolidatedExpenses
           | NonMatchingPeriodError
           | BVRError =>
        Forbidden(Json.toJson(errorResponse))
      case NotFoundError => NotFound(Json.toJson(errorResponse))
      case DownstreamError => InternalServerError(Json.toJson(errorResponse))
    }
  }

  private def auditEopsSubmission(nino: String,
                                  selfEmploymentId: String,
                                  fromDate: String,
                                  toDate: String,
                                  request: JsValue,
                                  correlationId: String,
                                  userDetails: UserDetails,
                                  eopsDeclarationAuditResponse: EopsDeclarationAuditResponse)
                                  (implicit ec: ExecutionContext,
                                        hc: HeaderCarrier): Future[AuditResult] = {

    val details = EopsDeclarationAuditDetail(
      userDetails.userType,
      userDetails.agentReferenceNumber,
      nino,
      fromDate,
      toDate,
      request,
      correlationId,
      selfEmploymentId,
      eopsDeclarationAuditResponse
    )

    val event = AuditEvent("submitEndOfPeriodStatement", "self-employment-submit-eops", details)

    auditService.auditEvent(event)
  }

}