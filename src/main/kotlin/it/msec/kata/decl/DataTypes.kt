package it.msec.kata.decl

import java.time.LocalDate

typealias FilePath = String
data class FileAccessError(val t: Throwable)

data class EntryColumns(val raw: String, val columns: List<String>)

data class Employee(val surname: String, val name: String, val email: EmailAddress, val birthday: LocalDate)

sealed class ProcessingResult
data class NothingToDo(val employee: Employee): ProcessingResult()
data class EmailReady(val email: Email): ProcessingResult()
data class CsvFormatError(val row: String): ProcessingResult()
data class ConversionError(val row: String): ProcessingResult()

inline class EmailAddress(val value: String)
data class Email(val to: EmailAddress, val subject: String, val body: String)

sealed class SendResult
data class Sent(val e: Email): SendResult()
data class NotSent(val e: Email): SendResult()
