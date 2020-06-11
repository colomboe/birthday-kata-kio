package it.msec.kata.decl

import it.msec.kata.birthday.executeProcessingResult
import it.msec.kio.*
import it.msec.kio.common.functions.identity
import it.msec.kio.common.list.sequence
import it.msec.kio.common.tuple.T2
import java.time.LocalDate
import java.time.format.DateTimeFormatter


fun readCsv(f: FilePath): KIO<FileEnv, FileAccessError, List<String>> =
        readLines(f).map { it.drop(1) }.mapError(::FileAccessError)

fun splitColumns(entry: String): EntryColumns =
        entry.split(",").map(String::trim).let { EntryColumns(entry, it) }

fun validateColumns(c: EntryColumns): IO<CsvFormatError, EntryColumns> =
        if (c.columns.size == 4) just(c) else failure(CsvFormatError(c.raw))

fun toEmailAction(e: Employee): URIO<TemplateEnv, EmailReady> =
        formatSubject(e)
                .flatMapT { _ -> formatBody(e) }
                .map { (subject, body) -> Email(e.email, subject, body) }
                .map(::EmailReady)

fun shouldSendEmail(today: LocalDate): (Employee) -> Boolean = { e ->
    today.dayOfMonth == e.birthday.dayOfMonth && today.month == e.birthday.month
}

fun filterBirthdayEmployee(t: T2<Employee, LocalDate>): IO<NothingToDo, Employee> {
    val (e, today) = t
    return just(e).filterTo(::NothingToDo, shouldSendEmail(today))
}

fun convertToEmployee(c: EntryColumns): IO<ConversionError, Employee> = unsafe {
    Employee(
            surname = c.columns[0],
            name = c.columns[1],
            email = EmailAddress(c.columns[3]),
            birthday = LocalDate.parse(c.columns[2], DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    )
}.mapError { ConversionError(c.raw) }

fun <R, A> treatErrorsAsProcessingResult(k: KIO<R, A, A>) = k.recover(::identity)





fun <R> execute(rs: List<ProcessingResult>): URIO<R, Unit> where R : LoggerEnv, R : MailSenderEnv =
        rs.map(::executeProcessingResult).sequence().map { Unit }


val defineActionForEmployee: (T2<Employee, LocalDate>) -> URIO<TemplateEnv, ProcessingResult> = (
        ::filterBirthdayEmployee
                then ::toEmailAction
                then ::treatErrorsAsProcessingResult
        )

fun <R> processEmployee(): (entry: String) -> URIO<R, Unit> where R : CalendarEnv, R : TemplateEnv, R: LoggerEnv, R: MailSenderEnv = (
        ::splitColumns
                then ::validateColumns
                then (::convertToEmployee with ::getToday)
                then defineActionForEmployee
                then ::treatErrorsAsProcessingResult
                then ::executeProcessingResult
        )

fun <R> process(es: List<String>): URIO<R, List<ProcessingResult>> where R : CalendarEnv, R : TemplateEnv =
        es.map(processEmployee()).sequence()

val main = (
        ::readCsv
                then ::process
                then ::execute
        )
