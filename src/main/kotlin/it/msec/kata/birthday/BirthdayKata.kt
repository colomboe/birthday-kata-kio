package it.msec.kata.birthday

import it.msec.kio.*
import it.msec.kio.common.list.sequence

fun main(csvFilePath: FilePath): KIO<KataEnv, FileAccessError, Unit> =
    readCsv(csvFilePath)
            .flatMap(::process)
            .flatMap(::execute)

fun <R> process(es: List<String>): URIO<R, List<ProcessingResult>> where R : CalendarEnv, R : TemplateEnv  =
    es.map(::processEmployee)
            .sequence()

fun <R> execute(rs: List<ProcessingResult>): URIO<R, Unit> where R : LoggerEnv, R : MailSenderEnv =
        rs.map(::executeProcessingResult)
                .sequence()
                .map { Unit }

fun readCsv(f: FilePath): KIO<FileEnv, FileAccessError, List<String>> =
        readLines(f)
                .map { it.drop(1) }
                .mapError(::FileAccessError)
