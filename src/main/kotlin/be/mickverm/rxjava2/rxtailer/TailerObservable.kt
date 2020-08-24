@file:JvmName("RxTailer")
@file:JvmMultifileClass

package be.mickverm.rxjava2.rxtailer

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
fun File.tail(): Observable<String> {
    return tail(Schedulers.io())
}

@SchedulerSupport(SchedulerSupport.CUSTOM)
fun File.tail(scheduler: Scheduler): Observable<String> {
    return TailerObservable(this, scheduler)
}

@SchedulerSupport(SchedulerSupport.IO)
fun File.tail(limit: Int): Observable<List<String>> {
    return tail(limit, Schedulers.io())
}

@SchedulerSupport(SchedulerSupport.CUSTOM)
fun File.tail(limit: Int, scheduler: Scheduler): Observable<List<String>> {
    return tail(scheduler).scan(emptyList(), { list, line ->
        list.toMutableList().apply {
            add(line)
        }.takeLast(limit)
    })
}

@SchedulerSupport(SchedulerSupport.IO)
fun <T : Any> File.tail(mapper: (line: String) -> T): Observable<T> {
    return tail(Schedulers.io(), mapper)
}

@SchedulerSupport(SchedulerSupport.CUSTOM)
fun <T : Any> File.tail(scheduler: Scheduler, mapper: (line: String) -> T): Observable<T> {
    return tail(scheduler).map { line ->
        mapper.invoke(line)
    }
}

@SchedulerSupport(SchedulerSupport.IO)
fun <T : Any> File.tail(limit: Int, mapper: (line: String) -> T): Observable<List<T>> {
    return tail(limit, Schedulers.io(), mapper)
}

@SchedulerSupport(SchedulerSupport.CUSTOM)
fun <T : Any> File.tail(limit: Int, scheduler: Scheduler, mapper: (line: String) -> T): Observable<List<T>> {
    return tail(scheduler, mapper).scan(emptyList(), { list, line ->
        list.toMutableList().apply {
            add(line)
        }.takeLast(limit)
    })
}

/**
 * @param file the file to follow.
 * @param scheduler the Scheduler on which the waiting happens and items are emitted
 */
private class TailerObservable(
    private val file: File,
    private val scheduler: Scheduler
) : Observable<String>() {

    override fun subscribeActual(observer: Observer<in String>) {
        val tailerObserver = TailerObserver(file, observer)
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

    private class TailerObserver(
        private val file: File,
        private val downstream: Observer<in String>
    ) : AtomicReference<Disposable>(), TailerListener, Disposable {

        val tailer = Tailer(file, this)

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
                downstream.onNext(line)
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
