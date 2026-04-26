package com.github.klee0kai.crossbox.core.proxies

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