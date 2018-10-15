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

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.EopsDeclarationSubmission
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._

import scala.concurrent.{ExecutionContext, Future}

class EopsDeclarationService @Inject()(connector: DesConnector){

  def submit(eopsDeclarationSubmission: EopsDeclarationSubmission)
            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ErrorResponse]] = {

    connector.submitEOPSDeclaration(eopsDeclarationSubmission.nino.nino, eopsDeclarationSubmission.from,
      eopsDeclarationSubmission.to, eopsDeclarationSubmission.selfEmploymentId).map {

      case Some(SingleError(error)) => Some(ErrorResponse(desErrorToMtdError(error.code), None))
      case Some(MultipleErrors(errors)) => Some(ErrorResponse(BadRequestError, Some(errors.map(_.code).map(desErrorToMtdError))))
      case Some(BVRErrors(errors)) =>
        if(errors.size == 1){
          Some(ErrorResponse(desBvrErrorToMtdError(errors.head.code), None))
        }else {
          Some(ErrorResponse(BVRError, Some(errors.map(_.code).map(desBvrErrorToMtdError))))
        }
      case Some(GenericError(error)) => Some(ErrorResponse(error, None))
      case _ => None
    }
  }

  private val desErrorToMtdError: Map[String, Error] = Map(
    "NOT_FOUND" -> NotFoundError,
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> NinoFormatError,
    "INVALID_ACCOUNTINGPERIODSTARTDATE" -> InvalidStartDateError,
    "INVALID_ACCOUNTINGPERIODENDDATE" -> InvalidEndDateError,
    "CONFLICT" -> ConflictError,
    "EARLY_SUBMISSION" -> EarlySubmissionError,
    "LATE_SUBMISSION" -> LateSubmissionError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> ServiceUnavailableError,
    "INVALID_INCOMESOURCEID" -> NotFoundError
  )

  private val desBvrErrorToMtdError: Map[String, Error] = Map(
    "C55008" -> RuleMismatchStartDate,
    "C55013" -> RuleMismatchEndDate,
    "C55014" -> RuleMismatchEndDate,
    "C55316" -> RuleConsolidatedExpenses,
    "C55317" -> RuleClass4Over16,
    "C55318" -> RuleClass4PensionAge
  )
}