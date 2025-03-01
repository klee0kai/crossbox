@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.processor.ksp.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.TargetFileProcessor
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue


class Processor(
    private val options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    companion object {
        val PROJECT_URL = "https://github.com/klee0kai/crossbox"
    }

    val targetProcessors = arrayOf<TargetFileProcessor>(
        CrossboxModelProcessor()
    )

    override fun process(
        resolver: Resolver
    ): List<KSAnnotated> = runBlocking {

        val symbolsForReprocessing = ConcurrentLinkedQueue<KSAnnotated>()
        val genSpecs = ConcurrentLinkedQueue<GenSpec>()

        val generateCodeJob = launch {
            targetProcessors.map { processor ->
                val symbols = processor.findSymbolsToProcess(resolver)
                symbolsForReprocessing.addAll(symbols.symbolsForReprocessing)
                genSpecs.addAll(
                    symbols.symbolsForProcessing.mapNotNull { targetSymbol ->
                        processor.process(
                            targetSymbol = targetSymbol,
                            resolver = resolver,
                            options = options,
                            logger = logger,
                        )
                    }
                )
            }
        }

        // join generate code
        // we provide separate file recording
        // with symbol resolution so that the processor can link the input and output of generation
        generateCodeJob.join()

        genSpecs.forEach { genSpec ->
            launch {
                genSpec?.fileSpec?.writeTo(
                    codeGenerator = codeGenerator,
                    dependencies = genSpec.dependencies
                )
            }
        }

        symbolsForReprocessing.toList()
    }

    override fun finish() {
        super.finish()
    }

    override fun onError() {
        super.onError()
    }


}