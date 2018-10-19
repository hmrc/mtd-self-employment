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

package v2.mocks.connectors

import java.time.LocalDate

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors.MtdError

import scala.concurrent.{ExecutionContext, Future}

class MockDesConnector extends MockFactory{

  val mockDesConnector = mock[DesConnector]

  object MockDesConnector {
    def submitEOPSDeclaration(nino: String, from: LocalDate, to: LocalDate, selfEmploymentId: String): CallHandler[Future[Option[MtdError]]] = {
      (mockDesConnector.submitEOPSDeclaration(_: String, _: LocalDate, _: LocalDate, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, from, to, selfEmploymentId, *, *)
    }
  }
}
