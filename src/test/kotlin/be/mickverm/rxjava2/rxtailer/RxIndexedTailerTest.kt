package be.mickverm.rxjava2.rxtailer

import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.*

internal class RxIndexedTailerTest {

    @TempDir
    lateinit var tmp: File

    @Test
    fun tailIndexed() {
        val file = File(tmp, "tail_indexed.txt")
        createFile(file, 2)

        val observer: TestObserver<Pair<Int, String>> = file.tailIndexed().test()
        observer.awaitCount(2)
            .assertValuesOnly(Pair(1, "Line 1"), Pair(2, "Line 2"))

        writeLines(file, 2, 3)

        observer.awaitCount(4)
            .assertValuesOnly(
                Pair(1, "Line 1"),
                Pair(2, "Line 2"),
                Pair(3, "Line 3"),
                Pair(4, "Line 4")
            ).cancel()
    }

    @Test
    fun tailIndexedFileNotFound() {
        val file = File(tmp, "tail_indexed_not_found.txt")

        val observer: TestObserver<Pair<Int, String>> = file.tailIndexed().test()
        observer.await().assertFailureAndMessage(
            FileNotFoundException::class.java,
            FileNotFoundException(file.path).message
        )
    }

    @Test
    fun tailIndexedLimited() {
        val file = File(tmp, "tail_indexed_limited.txt")
        createFile(file, 1)

        val observer: TestObserver<List<Pair<Int, String>>> = file.tailIndexed(2).test()
        observer.awaitCount(1)
            .assertValuesOnly(listOf(Pair(1, "Line 1")))

        writeLines(file, 2, 2)

        observer.awaitCount(3)
            .assertValuesOnly(
                listOf(Pair(1, "Line 1")),
                listOf(Pair(1, "Line 1"), Pair(2, "Line 2")),
                listOf(Pair(2, "Line 2"), Pair(3, "Line 3"))
            ).cancel()
    }

    @Test
    fun tailLimitedEmpty() {
        val file = File(tmp, "tail_indexed_limited_empty.txt")
        createFile(file)

        val observer: TestObserver<List<Pair<Int, String>>> = file.tailIndexed(2).test()
        observer.assertEmpty().cancel()
    }

    @Test
    fun tailIndexedMapped() {
        val file = File(tmp, "tail_indexed_mapped.txt")
        createFile(file, 3)

        val observer: TestObserver<String> = file.tailIndexed { index, line ->
            "$index: $line"
        }.test()

        observer.awaitCount(3)
            .assertValuesOnly("1: Line 1", "2: Line 2", "3: Line 3")
            .cancel()
    }

    @Test
    fun tailIndexedMappedPair() {
        val file = File(tmp, "tail_indexed_mapped_pair.txt")
        createFile(file, 3)

        val observer: TestObserver<String> = file.tailIndexed { pair ->
            "${pair.first}: ${pair.second}"
        }.test()

        observer.awaitCount(3)
            .assertValuesOnly("1: Line 1", "2: Line 2", "3: Line 3")
            .cancel()
    }

    @Test
    fun tailIndexedMappedException() {
        val file = File(tmp, "tail_indexed_mapped_exception.txt")
        createFile(file, 1)

        val observer: TestObserver<Int> = file.tailIndexed { index, line ->
            index + line.toInt()
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
    fun tailIndexedMappedPairException() {
        val file = File(tmp, "tail_indexed_mapped_pair_exception.txt")
        createFile(file, 1)

        val observer: TestObserver<Int> = file.tailIndexed { pair ->
            pair.first + pair.second.toInt()
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
        val file = File(tmp, "tail_indexed_limited_mapped.txt")
        createFile(file, 3)

        val observer: TestObserver<List<String>> = file.tailIndexed(2) { index, line ->
            "$index: $line"
        }.test()

        observer.awaitCount(3)
            .assertValuesOnly(
                listOf("1: Line 1"),
                listOf("1: Line 1", "2: Line 2"),
                listOf("2: Line 2", "3: Line 3"),
            ).cancel()
    }

    @Test
    fun tailLimitedMappedPair() {
        val file = File(tmp, "tail_indexed_limited_mapped_pair.txt")
        createFile(file, 3)

        val observer: TestObserver<List<String>> = file.tailIndexed(2) { pair ->
            "${pair.first}: ${pair.second}"
        }.test()

        observer.awaitCount(3)
            .assertValuesOnly(
                listOf("1: Line 1"),
                listOf("1: Line 1", "2: Line 2"),
                listOf("2: Line 2", "3: Line 3"),
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
