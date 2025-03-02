package com.github.klee0kai.crossbox.core.proxies

import com.github.klee0kai.crossbox.core.InvokeFunctionEvent
import com.github.klee0kai.crossbox.core.InvokeFunctionProcessor
import sun.awt.Mutex
import java.util.concurrent.Semaphore

open class CoroutineMutexProxy(
    val mutex: Mutex,
) : InvokeFunctionProcessor {

    override suspend fun startSuspendFunction(
        event: InvokeFunctionEvent,
    ): ((InvokeFunctionEvent) -> Unit)? {
        mutex.lock()
        return {
            mutex.unlock()
        }
    }

}