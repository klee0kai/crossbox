package com.github.klee0kai.crossbox.core

data class InvokeFunctionEvent(
    val name: String,
    val args: List<Any?> = emptyList(),
)

interface InvokeFunctionProcessor {

    /**
     * Handle the event of the function start and end.
     * Return the end handler for a specific function
     */
    fun startFunction(
        event: InvokeFunctionEvent,
    ): ((InvokeFunctionEvent) -> Unit)? {
        return null
    }

    /**
     * Handle the event of the function start and end.
     * Return the end handler for a specific function
     */
    suspend fun startSuspendFunction(
        event: InvokeFunctionEvent,
    ): (suspend (InvokeFunctionEvent) -> Unit)? {
        return null
    }

}
