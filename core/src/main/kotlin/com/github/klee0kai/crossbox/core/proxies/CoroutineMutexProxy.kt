package com.github.klee0kai.crossbox.core.proxies

import com.github.klee0kai.crossbox.core.InvokeFunctionEvent
import com.github.klee0kai.crossbox.core.InvokeFunctionProcessor
import kotlinx.coroutines.sync.Mutex

open class CoroutineMutexProxy(
    val mutex: Mutex,
) : InvokeFunctionProcessor {

    override suspend fun startSuspendFunction(
        event: InvokeFunctionEvent,
    ): (suspend (InvokeFunctionEvent) -> Unit)? {
        mutex.lock()
        return {
            mutex.unlock()
        }
    }

}