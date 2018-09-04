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

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors._

import scala.concurrent.{ExecutionContext, Future}

class SubmitEOPSDeclarationService @Inject()(desConnector: DesConnector) {

  def submitEOPSDeclaration(nino: String, from: LocalDate, to: LocalDate, selfEmploymentId: String)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ErrorResponse]] = {
    desConnector.submitEOPSDeclaration(nino, from, to, selfEmploymentId).map { _.map {
      case SingleError(desError)        => ErrorResponse(toClientError(desError), None)
      case MultipleErrors(desErrors)    => ErrorResponse(InvalidRequestError, Some(desErrors map toClientNonBvrError))
      case MultipleBVRErrors(desErrors) => ErrorResponse(BusinessError, Some(desErrors map toClientBVRError))
    }}
  }

  private val desErrorCodeToClientError: Map[String, Error] = Map(
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
    "SERVICE_UNAVAILABLE" -> ServiceUnavailableError
  )

  private val desBVRErrorCodeToClientError: Map[String, Error] = Map(
    "C55316" -> ConsolidatedExpensesError,
    "C55317" -> Class4Over16Error,
    "C55318" -> Class4PensionAgeError,
    "C55008" -> MismatchStartDateError,
    "C55013" -> MismatchEndDateError,
    "C55014" -> MismatchEndDateError
  )

  private def toClientError(error: Error): Error = (desErrorCodeToClientError ++ desBVRErrorCodeToClientError)(error.code)
  private def toClientNonBvrError(error: Error): Error = desErrorCodeToClientError(error.code)
  private def toClientBVRError(error: Error): Error = desBVRErrorCodeToClientError(error.code)
}
