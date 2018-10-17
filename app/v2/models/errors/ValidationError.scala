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

package v2.models.errors

case class ValidationError(code: String, message: String)

// Nino Errors
object NinoValidationError extends ValidationError("FORMAT_NINO", "The NINO format is invalid")

// Date Errors
object MissingStartDateError extends ValidationError("MISSING_START_DATE", "Missing account period start date")
object MissingEndDateError extends ValidationError("MISSING_END_DATE", "Missing account period end date")
object InvalidStartDateError extends ValidationError("FORMAT_START_DATE", "Invalid account period start date")
object InvalidEndDateError extends ValidationError("FORMAT_END_DATE", "Invalid account period end date")

