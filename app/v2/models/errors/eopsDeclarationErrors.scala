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

object InvalidPeriodStartDateError extends Error("FORMAT_START_DATE", "Invalid account period start date")
object InvalidPeriodEndDateError extends Error("FORMAT_END_DATE", "Invalid account period end date")
object AlreadySubmittedError extends Error("RULE_ALREADY_SUBMITTED", "You cannot submit a statement for the same accounting period twice")
object EarlySubmissionError extends Error("RULE_EARLY_SUBMISSION", "You cannot submit a statement before the end of your accounting period")
object LateSubmissionError extends Error("RULE_LATE_SUBMISSION", "The period to finalise has passed")
object ConsolidatedExpensesError extends Error("RULE_CONSOLIDATED_EXPENSES", "Consolidated expenses are not allowed if the cumulative turnover amount exceeds the threshold.")
object Class4Over16Error extends Error("RULE_CLASS4_OVER_16", "Class 4 exemption is not allowed because the individual's age is greater than or equal to 16 years old on the 6th April of the current tax year.")
object Class4PensionAgeError extends Error("RULE_CLASS4_PENSION_AGE", "Class 4 exemption is not allowed because the individual's age is less than their State Pension age on the 6th April of the current tax year.")
object MismatchStartDateError extends Error("RULE_MISMATCH_START_DATE", "The period submission start date must match the accounting period start date.")
object MismatchEndDateError extends Error("RULE_MISMATCH_END_DATE", "The period submission end date must match the accounting period end date.")
