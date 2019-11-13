package it.msec.kata.birthday

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import it.msec.kio.*
import it.msec.kio.ref.Ref
import it.msec.kio.result.Failure
import it.msec.kio.result.Result
import it.msec.kio.result.Success
import it.msec.kio.runtime.Runtime.unsafeRunSync
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDate.parse
import java.time.format.DateTimeFormatter.ofPattern

class AcceptanceTest {

    private val basePath = "src/test/resources/fixtures/"

    private val today = parse("2019/10/08", ofPattern("yyyy/MM/dd"))
    private val logRef = Ref("--- TEST LOG ---")

    private val testEnv: KataEnv = kataEnv(::Sent)
    private val failSendTestEnv: KataEnv = kataEnv(::NotSent)

    @Test
    fun `no file as input`() {
        val result = executeTestWithEnv("noFile.csv", testEnv)

        assertThat((result as Failure<FileAccessError>))
                .transform { it.error.t }
                .isInstanceOf(java.nio.file.NoSuchFileException::class)
    }

    @Test
    fun `goodFile as input`() {
        val result = executeTestWithEnv("goodFile.csv", testEnv)

        assertThat(result)
                .isInstanceOf(Success::class)
                .transform { it.value }
                .isEqualTo("""
                            --- TEST LOG ---
                            Successful sent mail to 'EmailAddress(value=john.doe@foobar.com)'
                            """
                        .trimIndent()
                )
    }

    @Test
    fun `goodFile as input (but send email error)`() {
        val result = executeTestWithEnv("goodFile.csv", failSendTestEnv)

        assertThat(result)
                .isInstanceOf(Success::class)
                .transform { it.value }
                .isEqualTo("""
                            --- TEST LOG ---
                            Error while sending mail to 'EmailAddress(value=john.doe@foobar.com)'
                            """
                        .trimIndent()
                )
    }

    @Test
    fun `emptyFile as input`() {
        val result = executeTestWithEnv("emptyFile.csv", testEnv)

        assertThat(result)
                .isInstanceOf(Success::class)
                .transform { it.value }
                .isEqualTo("""
                            --- TEST LOG ---
                            """
                        .trimIndent()
                )
    }

    @Test
    fun `bigFile as input`() {
        val result = executeTestWithEnv("bigFile.csv", testEnv)

        assertThat(result)
                .isInstanceOf(Success::class)
                .transform { it.value }
                .isEqualTo("""
                            --- TEST LOG ---
                            Successful sent mail to 'EmailAddress(value=john.doe@foobar.com)'
                            Successful sent mail to 'EmailAddress(value=fred.doe@foobar.com)'
                            Successful sent mail to 'EmailAddress(value=mark.doe@foobar.com)'
                            Successful sent mail to 'EmailAddress(value=alex.doe@foobar.com)'
                            Successful sent mail to 'EmailAddress(value=frank.doe@foobar.com)'
                            Successful sent mail to 'EmailAddress(value=ken.doe@foobar.com)'
                            Successful sent mail to 'EmailAddress(value=greg.doe@foobar.com)'
                            Successful sent mail to 'EmailAddress(value=dick.doe@foobar.com)'
                            """
                        .trimIndent()
                )
    }

    @Test
    fun `wrongFile as input`() {
        val result = executeTestWithEnv("wrongFile.csv", testEnv)

        assertThat(result)
                .isInstanceOf(Success::class)
                .transform { it.value }
                .isEqualTo("""
                            --- TEST LOG ---
                            Format error in '#comment'
                            Data conversion error in 'a,b,c,d'
                            Data conversion error in '1,2,3,4'
                            Format error in ''
                            Data conversion error in '5,6,7,8'
                            """
                        .trimIndent()
                )
    }

    fun executeTestWithEnv(fileName: String, env: KataEnv): Result<FileAccessError, String> {
        val prog = main("${basePath}$fileName").flatMap { logRef.get() }
        val result = unsafeRunSync(prog, env)
        return result
    }

    private fun kataEnv(sendResultConstructor: (Email) -> SendResult): KataEnv = object : KataEnv {
        override fun formatSubject(e: Employee): String = "Happy birthday"
        override fun formatBody(e: Employee): String = "Happy birthday, dear ${e.name}!"
        override fun today(): UIO<LocalDate> = just(today)
        override fun log(s: String): UIO<Unit> = logRef.update { log -> "$log\n$s" }
        override fun send(e: Email): UIO<SendResult> = just(sendResultConstructor(e))
        override fun readLines(f: FilePath): Task<List<String>> = unsafe { Files.readAllLines(Paths.get(f))!! }
    }

}
