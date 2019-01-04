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

package v2.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import v2.controllers.requestParsers.EopsDeclarationRequestDataParser
import v2.models.auth.UserDetails
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.inbound.EopsDeclarationRequestData
import v2.services.{EnrolmentsAuthService, EopsDeclarationService, MtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EopsDeclarationController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          eopsDeclarationService: EopsDeclarationService,
                                          requestDataParser: EopsDeclarationRequestDataParser,
                                          cc: ControllerComponents
                                         ) extends AuthorisedController(cc) {

  def submit(nino: String, selfEmploymentId: String, from: String, to: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val userDetails: UserDetails = request.userDetails

      requestDataParser.parseRequest(EopsDeclarationRequestData(nino, selfEmploymentId, from, to, AnyContentAsJson(request.body))) match {
        case Right(eopsDeclarationSubmission) =>
          eopsDeclarationService.submit(eopsDeclarationSubmission).map {
            case None => NoContent
            case Some(errorResponse) => processError(errorResponse)
          }
        case Left(validationErrorResponse) => Future {
          processError(validationErrorResponse)
        }
      }
    }

  private def processError(errorResponse: ErrorWrapper) = {
    errorResponse.error match {
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
           | BVRError =>
        Forbidden(Json.toJson(errorResponse))
      case NotFoundError => NotFound(Json.toJson(errorResponse))
      case DownstreamError => InternalServerError(Json.toJson(errorResponse))
    }
  }

}
