package it.msec.kata.decl

import it.msec.kio.*
import java.time.LocalDate

interface CalendarEnv {
    fun today(): UIO<LocalDate>
}

interface LoggerEnv {
    fun log(s: String): UIO<Unit>
}

interface MailSenderEnv {
    fun send(e: Email): UIO<SendResult>
}

interface TemplateEnv {
    fun formatSubject(e: Employee): String
    fun formatBody(e: Employee): String
}

interface FileEnv {
    fun readLines(f: FilePath): Task<List<String>>
}

interface KataEnv : CalendarEnv, LoggerEnv, MailSenderEnv, FileEnv, TemplateEnv

fun getToday(): URIO<CalendarEnv, LocalDate> = ask { env -> env.today() }
fun readLines(f: FilePath): RIO<FileEnv, List<String>> = ask { env -> env.readLines(f) }
fun log(s: String): URIO<LoggerEnv, Unit> = ask { env -> env.log(s) }
fun formatSubject(e: Employee): URIO<TemplateEnv, String> = askPure { env -> env.formatSubject(e) }
fun formatBody(e: Employee): URIO<TemplateEnv, String> = askPure { env -> env.formatBody(e) }
fun send(e: Email): URIO<MailSenderEnv, SendResult> = ask { env -> env.send(e) }
