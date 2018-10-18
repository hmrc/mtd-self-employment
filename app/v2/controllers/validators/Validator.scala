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

import v2.models.errors.ValidationError
import v2.models.inbound.InputData

trait Validator[A <: InputData] {

  type ValidationLevel[T] = T => List[ValidationError]

  def validate(data: A): List[ValidationError]

  // TODO Test
  protected def run[A <: InputData](validationSet: List[A => List[List[ValidationError]]], data: A): List[ValidationError] = {
    validationSet match {
      case Nil => List()
      case thisLevel :: remainingLevels => thisLevel(data) match {
        case x if x.nonEmpty => x.flatten
        case x if x.isEmpty => run(remainingLevels, data)
      }
    }
  }

}
