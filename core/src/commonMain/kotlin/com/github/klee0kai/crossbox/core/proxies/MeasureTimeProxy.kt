package com.github.klee0kai.crossbox.core.proxies

import kotlin.time.Clock
import kotlin.time.Duration

open class MeasureTimeProxy(
    val measureResult: (name: String, Duration) -> Unit = { _, _ -> },
) : InvokeFunctionProcessor {

    override fun startFunction(
        event: InvokeFunctionEvent,
    ): ((InvokeFunctionEvent) -> Unit)? {
        val start = Clock.System.now()
        return {
            val end = Clock.System.now()
            measureResult(event.name, (end - start))
        }
    }

    override suspend fun startSuspendFunction(
        event: InvokeFunctionEvent,
    ): (suspend (InvokeFunctionEvent) -> Unit)? {
        val start = Clock.System.now()
        return {
            val end = Clock.System.now()
            measureResult(event.name, (end - start))
        }
    }

}