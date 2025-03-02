package com.github.klee0kai.crossbox.core.proxies

import com.github.klee0kai.crossbox.core.InvokeFunctionEvent
import com.github.klee0kai.crossbox.core.InvokeFunctionProcessor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

open class MeasureTimeProxy(
    val measureResult: (name: String, Duration) -> Unit = { _, _ -> },
) : InvokeFunctionProcessor {

    override fun startFunction(
        event: InvokeFunctionEvent,
    ): ((InvokeFunctionEvent) -> Unit)? {
        val start = System.nanoTime()
        return {
            val end = System.nanoTime()
            measureResult(event.name, (end - start).nanoseconds)
        }
    }

    override suspend fun startSuspendFunction(
        event: InvokeFunctionEvent,
    ): (suspend (InvokeFunctionEvent) -> Unit)? {
        val start = System.nanoTime()
        return {
            val end = System.nanoTime()
            measureResult(event.name, (end - start).nanoseconds)
        }
    }

}