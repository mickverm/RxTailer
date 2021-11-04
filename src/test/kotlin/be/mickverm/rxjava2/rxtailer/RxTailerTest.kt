package be.mickverm.rxjava2.rxtailer

import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.*

internal class RxTailerTest {

    @TempDir
    lateinit var tmp: File

    @Test
    fun tail() {
        val file = File(tmp, "tail.txt")
        createFile(file, 2)

        val observer: TestObserver<String> = file.tail().test()
        observer.awaitCount(2)
            .assertValuesOnly("Line 1", "Line 2")

        writeLines(file, 2, 3)

        observer.awaitCount(4)
            .assertValuesOnly("Line 1", "Line 2", "Line 3", "Line 4")
            .cancel()
    }

    @Test
    fun tailFileNotFound() {
        val file = File(tmp, "tail_not_found.txt")

        val observer: TestObserver<String> = file.tail().test()
        observer.await().assertFailureAndMessage(
            FileNotFoundException::class.java,
            FileNotFoundException(file.path).message
        )
    }

    @Test
    fun tailLimited() {
        val file = File(tmp, "tail_limited.txt")
        createFile(file, 1)

        val observer: TestObserver<List<String>> = file.tail(2).test()
        observer.awaitCount(1)
            .assertValuesOnly(listOf("Line 1"))

        writeLines(file, 2, 2)

        observer.awaitCount(3)
            .assertValuesOnly(
                listOf("Line 1"),
                listOf("Line 1", "Line 2"),
                listOf("Line 2", "Line 3")
            ).cancel()
    }

    @Test
    fun tailLimitedEmpty() {
        val file = File(tmp, "tail_limited_empty.txt")
        createFile(file)

        val observer: TestObserver<List<String>> = file.tail(2).test()
        observer.assertEmpty().cancel()
    }

    @Test
    fun tailMapped() {
        val file = File(tmp, "tail_mapped.txt")
        createFile(file, 3)

        val observer: TestObserver<Int> = file.tail {
            it.split(" ")[1].toInt()
        }.test()

        observer.awaitCount(3)
            .assertValuesOnly(1, 2, 3)
            .cancel()
    }

    @Test
    fun tailMappedException() {
        val file = File(tmp, "tail_mapped_exception.txt")
        createFile(file, 1)

        val observer: TestObserver<Int> = file.tail {
            it.split(" ")[0].toInt()
        }.test()

        observer.await()
            .assertFailure(NumberFormatException::class.java)

        // Necessary in order to finish this test without exceptions.
        // Disposing the observable stops the Tailer which may not have been fully stopped by the time Jupiters TempDir
        // wants to remove the file (and thus causing this test to fail) because the Tailer is currently sleeping. The
        // Tailer will continue after its sleep timeout, the runnable will stop and the file will be released.
        Thread.sleep(1000)
    }

    @Test
    fun tailLimitedMapped() {
        val file = File(tmp, "tail_limited_mapped.txt")
        createFile(file, 3)

        val observer: TestObserver<List<Int>> = file.tail(2) {
            it.split(" ")[1].toInt()
        }.test()

        observer.awaitCount(3)
            .assertValuesOnly(
                listOf(1),
                listOf(1, 2),
                listOf(2, 3),
            ).cancel()
    }

    private fun createFile(file: File, count: Int = 0) {
        if (!file.parentFile.exists()) {
            throw IOException("Cannot create file $file as the parent directory does not exist.")
        }

        writeLines(file, count)
    }

    private fun writeLines(file: File, count: Int, start: Int = 1) {
        BufferedWriter(FileWriter(file, true)).use { out ->
            for (i in start until start + count) {
                out.appendLine("Line $i")
            }
        }
    }
}
