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

package v2.controllers.requestParsers.validators.validations

import v2.models.errors.{MtdError, SelfEmploymentIdError}
import v2.validations.NoValidationErrors

object SelfEmploymentIdFormatValidation extends Validation {

  private val incomeSourceIdRegex = "^X[A-Z0-9]{1}IS[0-9]{11}$"

  def validate(selfEmploymentId: String): List[MtdError] = {

    if (selfEmploymentId.matches(incomeSourceIdRegex)) NoValidationErrors else List(SelfEmploymentIdError)

  }

}
