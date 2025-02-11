package net.multun.gamecounter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class UniqueJobPool<T>(private val scope: CoroutineScope) {
    private val jobs = mutableMapOf<T, Job>()

    fun launch(key: T, block: suspend CoroutineScope.() -> Unit) {
        jobs.compute(key) { _, oldJob ->
            oldJob?.cancel()

            val newJob = scope.launch(block = block)
            newJob.invokeOnCompletion {
                jobs.remove(key, newJob)
            }
            newJob
        }
    }

    fun clear() {
        for (job in jobs.values)
            job.cancel()
        jobs.clear()
    }
}