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
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v2.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class EopsDeclarationControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String
    val from: String
    val to: String
    val selfEmploymentId: String

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(s"/2.0/ni/$nino/self-employments/$selfEmploymentId/end-of-period-statements/from/$from/to/$to")
    }
  }

  "Calling the sample endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {
        override val nino: String = "AA123456A"
        override val selfEmploymentId: String = "X1IS12345678901"
        override val from: String = "2018-01-01"
        override val to: String = "2018-12-31"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.submissionSuccess(nino, selfEmploymentId, from, to)
        }

        val submissionJson = Json.obj("finalised" -> true)

        val response: WSResponse = await(request().post(submissionJson))
        response.status shouldBe Status.NO_CONTENT

      }
    }
  }
}
