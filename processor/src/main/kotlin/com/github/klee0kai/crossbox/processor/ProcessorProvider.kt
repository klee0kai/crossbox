package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.processor.ksp.arch.TargetSymbolProcessor
import com.github.klee0kai.crossbox.processor.target.model.CrossboxModelProcessor
import com.github.klee0kai.crossbox.processor.target.model.CrossboxSerializableProcessor
import com.github.klee0kai.crossbox.processor.target.proxy.CrossboxAsyncInterfaceProcessor
import com.github.klee0kai.crossbox.processor.target.proxy.CrossboxGenInterfaceProcessor
import com.github.klee0kai.crossbox.processor.target.proxy.CrossboxProxyClassProcessor
import com.github.klee0kai.crossbox.processor.target.proxy.CrossboxSuspendInterfaceProcessor
import com.github.klee0kai.crossbox.processor.target.rsql.CrossboxRsqlFilterProcessor
import com.github.klee0kai.crossbox.processor.target.rsql.CrossboxRsqlFilterRegistryProcessor
import com.github.klee0kai.crossbox.processor.target.table.CrossboxJoineryDataFrameProcessor
import com.github.klee0kai.crossbox.processor.target.table.CrossboxJoineryRegistryProcessor
import com.github.klee0kai.crossbox.processor.target.table.CrossboxTableSawProcessor
import com.github.klee0kai.crossbox.processor.target.table.CrossboxTableSawRegistryProcessor
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
                CrossboxRsqlFilterRegistryProcessor(),
                CrossboxTableSawProcessor(),
                CrossboxTableSawRegistryProcessor(),
                CrossboxJoineryDataFrameProcessor(),
                CrossboxJoineryRegistryProcessor(),
                CrossboxSerializableProcessor(),
            ),
            options = environment.options,
            logger = environment.logger,
            codeGenerator = environment.codeGenerator,
        )
    }

}