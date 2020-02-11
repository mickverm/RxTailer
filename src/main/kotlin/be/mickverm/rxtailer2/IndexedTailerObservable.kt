@file:JvmName("RxTailer")
@file:JvmMultifileClass

package be.mickverm.rxtailer2

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.annotations.SchedulerSupport
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.internal.schedulers.TrampolineScheduler
import io.reactivex.schedulers.Schedulers
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListener
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.atomic.AtomicReference

@SchedulerSupport(SchedulerSupport.IO)
fun File.tailIndexed(): Observable<Pair<Long, String>> {
    return tailIndexed(Schedulers.io())
}

@SchedulerSupport(SchedulerSupport.CUSTOM)
fun File.tailIndexed(scheduler: Scheduler): Observable<Pair<Long, String>> {
    return IndexedTailerObservable(this, scheduler)
}

@SchedulerSupport(SchedulerSupport.IO)
fun File.tailIndexed(limit: Int): Observable<List<Pair<Long, String>>> {
    return tailIndexed(limit, Schedulers.io())
}

@SchedulerSupport(SchedulerSupport.CUSTOM)
fun File.tailIndexed(limit: Int, scheduler: Scheduler): Observable<List<Pair<Long, String>>> {
    return tailIndexed(scheduler).scan(emptyList(), { list, line ->
        list.toMutableList().apply {
            add(line)
            takeLast(limit)
        }
    })
}

@SchedulerSupport(SchedulerSupport.IO)
fun <T : Any> File.tailIndexed(mapper: (index: Long, line: String) -> T): Observable<T> {
    return tailIndexed(Schedulers.io(), mapper)
}

@SchedulerSupport(SchedulerSupport.CUSTOM)
fun <T : Any> File.tailIndexed(scheduler: Scheduler, mapper: (index: Long, line: String) -> T): Observable<T> {
    return tailIndexed(scheduler).map { pair ->
        mapper.invoke(pair.first, pair.second)
    }
}

@SchedulerSupport(SchedulerSupport.IO)
fun <T : Any> File.tailIndexed(limit: Int, mapper: (index: Long, line: String) -> T): Observable<List<T>> {
    return tailIndexed(limit, Schedulers.io(), mapper)
}

@SchedulerSupport(SchedulerSupport.CUSTOM)
fun <T : Any> File.tailIndexed(
    limit: Int,
    scheduler: Scheduler,
    mapper: (index: Long, line: String) -> T
): Observable<List<T>> {
    return tailIndexed(scheduler, mapper).scan(emptyList(), { list, line ->
        list.toMutableList().apply {
            add(line)
            takeLast(limit)
        }
    })
}

/**
 * @param file the file to follow.
 * @param scheduler the Scheduler on which the waiting happens and items are emitted
 */
private class IndexedTailerObservable(
    private val file: File,
    private val scheduler: Scheduler
) : Observable<Pair<Long, String>>() {

    override fun subscribeActual(observer: Observer<in Pair<Long, String>>) {
        val tailerObserver = IndexedTailerObserver(file, observer)
        observer.onSubscribe(tailerObserver)

        if (scheduler is TrampolineScheduler) {
            val worker = scheduler.createWorker()
            tailerObserver.setDisposable(worker)
            worker.schedule(tailerObserver.tailer)
        } else {
            val disposable = scheduler.scheduleDirect(tailerObserver.tailer)
            tailerObserver.setDisposable(disposable)
        }
    }

    private class IndexedTailerObserver(
        private val file: File,
        private val downstream: Observer<in Pair<Long, String>>
    ) : AtomicReference<Disposable>(), TailerListener, Disposable {

        val tailer = Tailer(file, this)

        private var index = 0L

        override fun dispose() {
            DisposableHelper.dispose(this)
        }

        override fun isDisposed(): Boolean {
            return get() == DisposableHelper.DISPOSED
        }

        override fun init(tailer: Tailer) {

        }

        override fun fileNotFound() {
            if (get() != DisposableHelper.DISPOSED) {
                downstream.onError(FileNotFoundException(file.path))
            }
        }

        override fun fileRotated() {

        }

        override fun handle(line: String) {
            if (get() != DisposableHelper.DISPOSED) {
                downstream.onNext(Pair(index++, line))
            }
        }

        override fun handle(ex: Exception) {
            if (get() != DisposableHelper.DISPOSED) {
                downstream.onError(ex)
            }
        }

        fun setDisposable(disposable: Disposable) {
            DisposableHelper.setOnce(this, disposable)
        }
    }
}
