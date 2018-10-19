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

package v2.controllers.validators

import java.time.LocalDate

import v2.controllers.validators.validations._
import v2.models.errors._
import v2.models.inbound.{EopsDeclaration, EopsDeclarationInputData}

class EopsDeclarationInputDataValidator extends Validator[EopsDeclarationInputData] {

  private val validationSet = List(levelOneValidations, levelTwoValidations, levelThreeValidations)

  private def levelOneValidations: EopsDeclarationInputData => List[List[ValidationError]] = (data: EopsDeclarationInputData) => {
    List(
      NinoValidation.validate(data.nino),
      NonEmptyValidation.validate(data.from, MissingStartDateError),
      NonEmptyValidation.validate(data.to, MissingEndDateError),
      DateFormatValidation.validate(data.from, InvalidStartDateError),
      DateFormatValidation.validate(data.to, InvalidEndDateError),
      SelfEmploymentIdFormatValidation.validate(data.selfEmploymentId)
    )
  }

  private def levelTwoValidations: EopsDeclarationInputData => List[List[ValidationError]] = (data: EopsDeclarationInputData) => {
    List(
      DateRangeValidation.validate(LocalDate.parse(data.from), LocalDate.parse(data.to)),
      JsonFormatValidation.validate[EopsDeclaration](data.body)
    )
  }

  private def levelThreeValidations: EopsDeclarationInputData => List[List[ValidationError]] = (data: EopsDeclarationInputData) => {
    List(
      EopsDeclarationRequestDataValidation.validate(data.body)
    )
  }

  override def validate(data: EopsDeclarationInputData): List[ValidationError] = {
    run(validationSet, data)
  }


}