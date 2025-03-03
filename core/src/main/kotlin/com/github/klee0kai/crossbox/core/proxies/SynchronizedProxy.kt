package com.github.klee0kai.crossbox.core.proxies

import com.github.klee0kai.crossbox.core.InvokeFunctionEvent
import com.github.klee0kai.crossbox.core.InvokeFunctionProcessor
import java.util.concurrent.locks.ReentrantLock

open class SynchronizedProxy(
    val mutex: ReentrantLock = ReentrantLock(),
) : InvokeFunctionProcessor {

    override fun startFunction(
        event: InvokeFunctionEvent,
    ): ((InvokeFunctionEvent) -> Unit)? {
        mutex.lock()
        return {
            mutex.unlock()
        }
    }

}