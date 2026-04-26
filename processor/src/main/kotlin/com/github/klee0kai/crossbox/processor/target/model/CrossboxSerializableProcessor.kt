package com.github.klee0kai.crossbox.processor.target.model

import com.github.klee0kai.crossbox.core.CrossboxSerializableRegistry
import com.github.klee0kai.crossbox.processor.common.findCommonPgk
import com.github.klee0kai.crossbox.processor.exceptions.forEachKsNode
import com.github.klee0kai.crossbox.processor.ksp.arch.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.arch.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.arch.TargetSymbolProcessor
import com.github.klee0kai.crossbox.processor.poet.*
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

class CrossboxSerializableProcessor : TargetSymbolProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxSerializableRegistry::class.asClassName().canonicalName)
            .groupBy { it.validate() }

        val validSymbols = annotatedSymbols[true].orEmpty()

        return SymbolsToProcess(
            symbolsForProcessing = validSymbols,
            symbolsForReprocessing = annotatedSymbols[false].orEmpty(),
            processOnlyTogether = true,
        )
    }

    override suspend fun multiSymbolsProcess(
        targetSymbols: List<KSAnnotated>,
        resolver: Resolver,
        options: Map<String, String>,
        logger: KSPLogger
    ): GenSpec? {
        if (targetSymbols.isEmpty()) {
            return null
        }
        val allKsFiles = targetSymbols.mapNotNull { it.containingFile }

        val commonPkg = targetSymbols
            .mapNotNull { it.containingFile?.packageName?.asString() }
            .findCommonPgk()


        // Generate registry file with all collected serializable classes
        val fileSpec = genFileSpec(
            packageName = commonPkg.crossboxPackageName,
            fileName = "SerializerRegistry"
        ) {
            genLibComment()

            // Generate property with map of serializers
            genProperty(
                name = "serializerRegistry",
                type = Map::class.asClassName()
                    .parameterizedBy(
                        KClass::class.asClassName().parameterizedBy(STAR),
                        KSerializer::class.asClassName().parameterizedBy(STAR)
                    ),
                KModifier.PUBLIC
            ) {
                genGetter {
                    addCode("return mapOf(\n")
                    targetSymbols.forEachKsNode { _, classDecl ->
                        val className = (classDecl as KSClassDeclaration).toClassName()
                        addCode(
                            "%T::class to %T.serializer(),\n",
                            className,
                            className
                        )
                    }
                    addCode(")")
                }
            }
        }

        return GenSpec(
            fileSpec = fileSpec,
            dependencies = Dependencies(
                aggregating = true,
                *allKsFiles.toTypedArray()
            ),
        )
    }

}