package it.msec.kata.birthday

import it.msec.kio.URIO
import it.msec.kio.flatMap
import it.msec.kio.just

fun <R> executeProcessingResult(pr: ProcessingResult): URIO<R, Unit> where R : LoggerEnv, R : MailSenderEnv =
        when (pr) {
            is NothingToDo -> just(Unit)
            is CsvFormatError -> log("Format error in '${pr.row}'")
            is ConversionError -> log("Data conversion error in '${pr.row}'")
            is EmailReady -> executeSend(pr.email)
        }

fun <R> executeSend(email: Email): URIO<R, Unit> where R : MailSenderEnv, R : LoggerEnv =
        send(email)
                .flatMap {
                    when (it) {
                        is Sent -> log("Successful sent mail to '${email.to}'")
                        is NotSent -> log("Error while sending mail to '${email.to}'")
                    }
                }
