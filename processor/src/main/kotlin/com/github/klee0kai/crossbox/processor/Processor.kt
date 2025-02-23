package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.core.Crossbox
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.asClassName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class Processor(
    private val options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    override fun process(
        resolver: Resolver
    ): List<KSAnnotated> = runBlocking {
        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(Crossbox::class.asClassName().canonicalName)
            .groupBy { it.validate() }
        val validSymbols = annotatedSymbols[true].orEmpty()
        val symbolsForReprocessing = annotatedSymbols[false].orEmpty()

        validSymbols.forEach { validSymbol ->
            launch(Dispatchers.Default) {
                val fileOwner = validSymbol.containingFile ?: return@launch
                val classDeclaration = validSymbol as? KSClassDeclaration ?: return@launch
                val file = codeGenerator.createNewFile(
                    // Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
                    // Learn more about incremental processing in KSP from the official docs:
                    // https://kotlinlang.org/docs/ksp-incremental.html
                    dependencies = Dependencies(false, fileOwner),
                    packageName = fileOwner.packageName.asString(),
                    fileName = "${classDeclaration.simpleName.getShortName()}CrossboxExt"
                )


            }
        }



        symbolsForReprocessing
    }

    override fun finish() {
        super.finish()
    }

    override fun onError() {
        super.onError()
    }


}