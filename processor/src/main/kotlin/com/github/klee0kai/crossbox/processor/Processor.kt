package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.processor.ksp.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.TargetFileProcessor
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min


class Processor(
    private val options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    companion object {
        const val PROJECT_URL = "https://github.com/klee0kai/crossbox"

        val ONE_RUN_SYMBOLS_COUNT = max(Runtime.getRuntime().availableProcessors(), 4)
    }

    val targetProcessors = arrayOf<TargetFileProcessor>(
        CrossboxModelProcessor(),
        CrossboxGenInterfaceProcessor(),
    )

    override fun process(
        resolver: Resolver
    ): List<KSAnnotated> = runBlocking(Dispatchers.Default) {

        val globalSymbolsForProcessing = ConcurrentLinkedQueue<KSAnnotated>()
        val globalSymbolsForReprocessing = ConcurrentLinkedQueue<KSAnnotated>()
        val genSpecs = ConcurrentLinkedQueue<GenSpec>()
        val processSymbolsCounter = AtomicInteger(0)

        val generateCodeJob = launch {
            targetProcessors.map { processor ->
                val symbols = processor.findSymbolsToProcess(resolver)
                var takeSymbolsCount = 0
                processSymbolsCounter.updateAndGet { totalCount ->
                    takeSymbolsCount = min(symbols.symbolsForProcessing.size, ONE_RUN_SYMBOLS_COUNT - totalCount)
                    takeSymbolsCount = max(takeSymbolsCount, 0)
                    totalCount + takeSymbolsCount
                }

                val symbolsForReprocessing = (symbols.symbolsForReprocessing
                        + symbols.symbolsForProcessing.drop(takeSymbolsCount))
                val symbolsForProcessing = symbols.symbolsForProcessing.take(takeSymbolsCount)

                globalSymbolsForReprocessing.addAll(symbolsForReprocessing)
                globalSymbolsForProcessing.addAll(symbolsForProcessing)

                genSpecs.addAll(
                    symbolsForProcessing
                        .mapNotNull { targetSymbol ->
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
            genSpec?.fileSpec?.writeTo(
                codeGenerator = codeGenerator,
                dependencies = genSpec.dependencies
            )
        }

        globalSymbolsForReprocessing.toList()
    }

    override fun finish() {
        super.finish()
    }

    override fun onError() {
        super.onError()
    }


}