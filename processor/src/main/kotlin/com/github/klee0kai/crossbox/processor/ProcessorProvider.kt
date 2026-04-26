package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.processor.ksp.arch.TargetSymbolProcessor
import com.github.klee0kai.crossbox.processor.target.*
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TargetKSPProcessor(
            targetProcessors = arrayOf<TargetSymbolProcessor>(
                CrossboxGenInterfaceProcessor(),
                CrossboxModelProcessor(),
                CrossboxSuspendInterfaceProcessor(),
                CrossboxAsyncInterfaceProcessor(),
                CrossboxProxyClassProcessor(),
                CrossboxRsqlFilterProcessor(),
                CrossboxTableSawProcessor(),
                CrossboxJoineryDataFrameProcessor(),
                CrossboxSerializableProcessor(),
            ),
            options = environment.options,
            logger = environment.logger,
            codeGenerator = environment.codeGenerator,
        )
    }

}