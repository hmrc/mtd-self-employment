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

package v2.connectors.httpparsers

import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import v2.models.errors.{GenericError, NotFoundError, ServiceUnavailableError, _}

object SubmitEOPSDeclarationHttpParser extends HttpParser {

  val logger = Logger(SubmitEOPSDeclarationHttpParser.getClass)

  implicit val submitEOPSDeclarationHttpReads: HttpReads[Option[DesError]] =
    new HttpReads[Option[DesError]] {
      override def read(method: String, url: String, response: HttpResponse): Option[DesError] = {

        if(response.status != NO_CONTENT) {
          logger.info("[SubmitEOPSDeclarationHttpParser][read] - " +
            s"Error response received from DES with status: ${response.status} and body\n" +
            s"${response.body} when calling $url")
        }
        response.status match {

          case NO_CONTENT => None
          case BAD_REQUEST | FORBIDDEN | CONFLICT => Some(parseErrors(response))
          case NOT_FOUND => Some(GenericError(NotFoundError))
          case SERVICE_UNAVAILABLE => Some(GenericError(ServiceUnavailableError))
          case _ => Some(GenericError(DownstreamError))
        }
      }
    }
}
