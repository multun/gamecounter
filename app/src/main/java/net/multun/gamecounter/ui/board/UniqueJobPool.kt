package net.multun.gamecounter.ui.board

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


data object OverrideCancellation : CancellationException(null) {
    private fun readResolve(): Any = OverrideCancellation
}

class UniqueJobPool<T>(private val scope: CoroutineScope) {
    private val jobs = mutableMapOf<T, Job>()

    fun launch(key: T, block: suspend CoroutineScope.() -> Unit) {
        jobs.compute(key) { _, oldJob ->
            // stop the old job and invoke its completion handler
            oldJob?.cancel(OverrideCancellation)

            // start the new job
            val newJob = scope.launch(block = block)

            // setup the new job to remove itself from the map once completed
            newJob.invokeOnCompletion {
                // do not attempt to automatically remove the job on cancellation,
                // as it will cause concurrent modifications:
                // jobs.compute() -> oldJob.cancel() -> jobs.remove(key, oldJob)
                if (it == OverrideCancellation)
                    return@invokeOnCompletion
                jobs.remove(key, newJob)
            }
            newJob
        }
    }

    fun cancelAll() {
        for (job in jobs.values)
            job.cancel()
    }
}