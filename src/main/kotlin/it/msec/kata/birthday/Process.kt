@file:Suppress("FunctionName")

package it.msec.kata.birthday

import it.msec.kio.*
import it.msec.kio.common.composition.then
import it.msec.kio.common.composition.with
import it.msec.kio.common.functions.identity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun <R, T> `merge error and success results`(e: KIO<R, T, T>): URIO<R, T> = e.recover(::identity)

fun <R> processEmployee(entry: String): URIO<R, ProcessingResult> where R : CalendarEnv, R : TemplateEnv = (
        ::`split entry columns`
                then ::`validate column values`
                then ::`convert to employee`
                then ::`define the action to execute`
                then ::`merge error and success results`
        )(entry)

fun <R> `define the action to execute`(employee: Employee): URIO<R, ProcessingResult> where R : CalendarEnv, R : TemplateEnv = (
        ::`take only if it's the employee birthday` with ::`today date`
                then ::`generate email content`
                then ::`define the send email action`
                then ::`merge error and success results`
        )(employee)

fun `define the send email action`(e: Email) = EmailReady(e)

fun `take only if it's the employee birthday`(e: Employee, date: LocalDate): IO<ProcessingResult, Employee> =
        just(e).filterTo(::NothingToDo, isBirthday(date))

fun `generate email content`(e: Employee): URIO<TemplateEnv, Email> =
        formatSubject(e)
                .flatMapT { _ -> formatBody(e) }
                .map { (subject, body) -> Email(e.email, subject, body) }

fun isBirthday(today: LocalDate): (Employee) -> Boolean = { e ->
    today.dayOfMonth == e.birthday.dayOfMonth
            && today.month == e.birthday.month
}

fun `convert to employee`(c: EntryColumns): IO<ConversionError, Employee> = unsafe {
    Employee(
            surname = c.columns[0],
            name = c.columns[1],
            email = EmailAddress(c.columns[3]),
            birthday = LocalDate.parse(c.columns[2], DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    )
}.mapError { ConversionError(c.raw) }

fun `split entry columns`(entry: String): EntryColumns =
        entry.split(",")
                .map(String::trim)
                .let { EntryColumns(entry, it) }

fun `validate column values`(c: EntryColumns): IO<CsvFormatError, EntryColumns> =
        if (c.columns.size == 4)
            just(c)
        else
            failure(CsvFormatError(c.raw))
