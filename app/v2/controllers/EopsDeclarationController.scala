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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import v2.models.EopsDeclarationSubmission
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.services.{EnrolmentsAuthService, EopsDeclarationService, MtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class EopsDeclarationController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          eopsDeclarationService: EopsDeclarationService
                                         ) extends AuthorisedController {

  def submit(nino: String, selfEmploymentId: String, from: String, to: String): Action[AnyContent] = authorisedAction(nino).async { implicit request =>

    // TODO Validator bit
    // Assuming validator response was success

    val validatorResponse = EopsDeclarationSubmission(Nino(nino), selfEmploymentId, LocalDate.parse(from), LocalDate.parse(to))

    eopsDeclarationService.submit(validatorResponse).map {
      case None => NoContent
      case Some(errorResponse) => processError(errorResponse)
    }

  }

  private def processError(errorResponse: ErrorWrapper) = {
    errorResponse.error match {
      case InvalidStartDateError
           | InvalidEndDateError
           | InvalidRangeError
           | BadRequestError
           | NinoFormatError
           | EarlySubmissionError
           | NinoFormatError
           | LateSubmissionError =>
        BadRequest(Json.toJson(errorResponse))
      case ConflictError
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
