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

object SubmitEopsDeclarationErrors {



  object MissingStartDateError extends Error("MISSING_START_DATE", "Missing account period start date")

  object NinoFormatError extends Error("FORMAT_NINO", "The NINO format is invalid")

  object InvalidStartDateError extends Error("FORMAT_START_DATE", "Invalid account period start date")

  object MissingEndDateError extends Error("MISSING_END_DATE", "Missing account period end date")

  object InvalidEndDateError extends Error("FORMAT_END_DATE", "Invalid account period end date")

  object InvalidRangeError extends Error("RANGE_INVALID", "The start date must be the same day or before the from date")

  object NotFinalisedDeclaration extends Error("INVALID_REQUEST", "The statement cannot be accepted without a declaration that it is finalised.")

  object ConflictError extends Error("RULE_ALREADY_SUBMITTED", "You cannot submit a statement for the same accounting period twice")

  object EarlySubmissionError extends Error("RULE_EARLY_SUBMISSION", "You cannot submit a statement before the end of your accounting period")

  object LateSubmissionError extends Error("RULE_LATE_SUBMISSION", "The period to finalise has passed")


  //  BVR Errors

  object RuleClass4Over16 extends Error("RULE_CLASS4_OVER_16",
    "Class 4 exemption is not allowed because the individual's age is greater than or equal to 16 years old on the 6th April of the current tax year.")

  object RuleClass4PensionAge extends Error("RULE_CLASS4_PENSION_AGE",
    "Class 4 exemption is not allowed because the individual's age is less than their State Pension age on the 6th April of the current tax year.")

  object RuleMismatchStartDate extends Error("RULE_MISMATCH_START_DATE",
    "The period submission start date must match the accounting period start date.")

  object RuleMismatchEndDate extends Error("RULE_MISMATCH_END_DATE",
    "The period submission end date must match the accounting period end date.")

  object RuleConsolidatedExpenses extends Error("RULE_CONSOLIDATED_EXPENSES",
    "Consolidated expenses are not allowed if the cumulative turnover amount exceeds the threshold.")

}

