package it.msec.kata.birthday

import it.msec.kio.*
import it.msec.kio.common.composition.andThen
import it.msec.kio.common.functions.identity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun <R> processEmployee(entry: String): URIO<R, ProcessingResult> where R : CalendarEnv, R : TemplateEnv =
        (::splitColumns andThen ::validateColumns)(entry)
                .flatMap(::toEmployee)
                .flatMap(::toAction)
                .recover(::identity)

fun <R> toAction(employee: Employee): URIO<R, ProcessingResult> where R : CalendarEnv, R : TemplateEnv =
    getToday().flatMap { today ->
        just(employee)
                .filterTo(::NothingToDo, isBirthday(today))
                .flatMap(::toEmail)
                .map(::EmailReady)
                .recover(::identity)
    }

fun toEmail(e: Employee): URIO<TemplateEnv, Email> =
        formatSubject(e)
                .flatMapT { _ -> formatBody(e) }
                .map { (subject, body) -> Email(e.email, subject, body) }

fun isBirthday(today: LocalDate): (Employee) -> Boolean = { e ->
    today.dayOfMonth == e.birthday.dayOfMonth
            && today.month == e.birthday.month
}

fun toEmployee(c: EntryColumns): IO<ConversionError, Employee> = unsafe {
            Employee(
                    surname = c.columns[0],
                    name = c.columns[1],
                    email = EmailAddress(c.columns[3]),
                    birthday = LocalDate.parse(c.columns[2], DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            )
        }.mapError { ConversionError(c.raw) }

fun splitColumns(entry: String): EntryColumns =
        entry.split(",")
                .map(String::trim)
                .let { EntryColumns(entry, it) }

fun validateColumns(c: EntryColumns): IO<CsvFormatError, EntryColumns> =
        if (c.columns.size == 4)
            just(c)
        else
            failure(CsvFormatError(c.raw))
