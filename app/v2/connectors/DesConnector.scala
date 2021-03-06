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

package v2.connectors

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.HttpClient
import v2.config.AppConfig
import v2.models.outcomes.EopsDeclarationOutcome

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(http: HttpClient,
                             appConfig: AppConfig) {

  val logger: Logger = Logger(this.getClass)

  private[connectors] def desHeaderCarrier(implicit hc: HeaderCarrier, correlationId: String): HeaderCarrier = hc
    .copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))
    .withExtraHeaders("Environment" -> appConfig.desEnv, "Content-Type" -> "application/json", "CorrelationId" -> correlationId)

  def submitEOPSDeclaration(nino: String, from: LocalDate, to: LocalDate, selfEmploymentId: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    correlationId: String): Future[EopsDeclarationOutcome] = {

    import v2.connectors.httpparsers.SubmitEOPSDeclarationHttpParser.submitEOPSDeclarationHttpReads

    val url = s"${appConfig.desBaseUrl}/income-tax/income-sources/nino/$nino/self-employment/$from/$to/declaration?incomeSourceId=$selfEmploymentId"

    http.POSTEmpty[EopsDeclarationOutcome](url)(submitEOPSDeclarationHttpReads, desHeaderCarrier, implicitly)
  }
}