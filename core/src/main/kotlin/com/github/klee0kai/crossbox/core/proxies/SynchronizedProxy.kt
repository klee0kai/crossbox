package com.github.klee0kai.crossbox.core.proxies

import com.github.klee0kai.crossbox.core.InvokeFunctionEvent
import com.github.klee0kai.crossbox.core.InvokeFunctionProcessor

open class SynchronizedProxy(
    val mutex: Object,
) : InvokeFunctionProcessor {

    override fun startFunction(
        event: InvokeFunctionEvent,
    ): ((InvokeFunctionEvent) -> Unit)? {
        mutex.wait()
        return {
            mutex.notify()
        }
    }

}