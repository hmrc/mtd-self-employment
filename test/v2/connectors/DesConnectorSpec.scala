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

package v2.connectors

import java.time.LocalDate

import play.api.http.HeaderNames
import v2.mocks.{MockAppConfig, MockHttpClient}
import v2.models.errors._

import scala.concurrent.Future

class DesConnectorSpec extends ConnectorSpec {

  val baseUrl = "test-mtdIdBaseUrl"

  private trait Test extends MockHttpClient with MockAppConfig {

    val connector = new DesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnv returns "des-environment"
  }

  "desHeaderCarrier" should {
    "return a header carrier with an authorization header using the DES token specified in config" in new Test {
      connector.desHeaderCarrier.headers.contains(HeaderNames.AUTHORIZATION -> "Bearer des-token") shouldBe true
    }

    "return a header carrier with an environment header using the DES environment specified in config" in new Test {
      connector.desHeaderCarrier.headers.contains("Environment" -> "des-environment") shouldBe true
    }
  }

  "submitEOPSDeclaration" should {

    val nino = "AA12356A"
    val from = LocalDate.parse("2017-01-01")
    val to = LocalDate.parse("2018-01-01")
    val selfEmploymentId = "test-se-id"

    val url = s"$baseUrl/income-tax/income-sources/nino/$nino/self-employments/$from/$to/declaration?incomeSourceId=$selfEmploymentId"

    "return a None" when {
      "the http client returns None" in new Test {
        MockedHttpClient.postEmpty[Option[DesError]](url)
          .returns(Future.successful(None))

        val result: Option[DesError] = await(connector.submitEOPSDeclaration(nino, from, to, selfEmploymentId))
        result shouldBe None
      }
    }

    "return an ErrorResponse" when {
      "the http client returns an error response" in new Test {
        val errorResponse = SingleError(InvalidNinoError)

        MockedHttpClient.postEmpty[Option[DesError]](url)
          .returns(Future.successful(Some(errorResponse)))

        val result: Option[DesError] = await(connector.submitEOPSDeclaration(nino, from, to, selfEmploymentId))
        result shouldBe Some(errorResponse)
      }
    }
  }
}
