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

package v2.models

import play.api.libs.json.Json
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class EOPSDeclarationSpec extends UnitSpec with JsonErrorValidators {

  "reads" should {
    "return an EOPSDeclaration model" when {
      "correct json is supplied and the value for 'finalised' is true" in {
        val json = Json.parse("""{ "finalised": true }""")

        val model = EOPSDeclaration.reads.reads(json).get
        model shouldBe EOPSDeclaration(true)
      }

      "correct json is supplied and the value for 'finalised' is false" in {
        val json = Json.parse("""{ "finalised": false }""")

        val model = EOPSDeclaration.reads.reads(json).get
        model shouldBe EOPSDeclaration(false)
      }
    }

    val eopsDeclarationJson = Json.parse("""{ "finalised": true }""")

    testMandatoryProperty[EOPSDeclaration](eopsDeclarationJson)("/finalised")

    testPropertyType[EOPSDeclaration](eopsDeclarationJson)(
      path = "/finalised",
      replacement = 1.toJson,
      expectedError = JsonError.BOOLEAN_FORMAT_EXCEPTION
    )
  }
}
