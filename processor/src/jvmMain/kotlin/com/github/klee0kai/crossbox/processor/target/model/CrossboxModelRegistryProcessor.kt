@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target.model

import com.github.klee0kai.crossbox.core.CrossboxModel
import com.github.klee0kai.crossbox.core.field.CrossBoxModelRegistry
import com.github.klee0kai.crossbox.processor.common.findCommonPgk
import com.github.klee0kai.crossbox.processor.exceptions.forEachKsNode
import com.github.klee0kai.crossbox.processor.ksp.arch.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.arch.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.arch.TargetSymbolProcessor
import com.github.klee0kai.crossbox.processor.poet.*
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

class CrossboxModelRegistryProcessor : TargetSymbolProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxModel::class.asClassName().canonicalName)
            .filter { it.getAnnotationsByType(CrossboxModel::class).firstOrNull()?.commonRegistry == true }


        return SymbolsToProcess(
            symbolsForProcessing = annotatedSymbols.toList(),
            symbolsForReprocessing = emptyList(),
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

        val commonPkg = targetSymbols
            .mapNotNull { it.containingFile?.packageName?.asString() }
            .findCommonPgk()
        val implClName = ClassName(commonPkg.crossboxPackageName, "CrossBoxModelRegistryImpl")
        val registryInterface = CrossBoxModelRegistry::class.asClassName()

        // Generate registry file with all collected model classes
        val fileSpec = genFileSpec(implClName.packageName, implClName.simpleName) {
            genLibComment()

            genClass(implClName) {
                addSuperinterface(registryInterface)

                genProperty(
                    name = "modelRegistry",
                    type = Map::class.asClassName()
                        .parameterizedBy(
                            KClass::class.asClassName().parameterizedBy(STAR),
                            registryInterface.nestedClass("ModelTool").parameterizedBy(STAR)
                        ),
                    KModifier.OVERRIDE
                ) {
                    genGetter {
                        addCode("return mapOf(\n")
                        targetSymbols.forEachKsNode { _, classDecl ->
                            val className = (classDecl as KSClassDeclaration).toClassName()
                            val crossboxModelAnn = classDecl.getAnnotationsByType(CrossboxModel::class)
                                .firstOrNull()

                            addCode("%T::class to %T(\n", className, registryInterface.nestedClass("ModelTool"))
                            addCode("  type = %T::class,\n", className)

                            if (crossboxModelAnn?.fieldList == true) {
                                addCode(
                                    "  crossboxFieldList = { _ -> %T.crossboxFieldList },\n",
                                    className,
                                )
                            } else {
                                addCode("  crossboxFieldList = null,\n")
                            }

                            if (crossboxModelAnn?.merge == true) {
                                addCode("  merge = { obj, pair -> obj.merge(pair) },\n")
                            } else {
                                addCode("  merge = { _, _ -> throw UnsupportedOperationException(\"merge not enabled for %T\") },\n", className)
                            }

                            if (crossboxModelAnn?.merge == true) {
                                addCode("  deepMerge = { obj, pair -> obj.deepMerge(pair) },\n")
                            } else {
                                addCode("  deepMerge = { _, _ -> throw UnsupportedOperationException(\"deepMerge not enabled for %T\") },\n", className)
                            }

                            addCode("),\n")
                        }
                        addCode(")")
                    }
                }
            }
        }

        return GenSpec(
            fileSpec = fileSpec,
            dependencies = Dependencies(
                aggregating = true,
                *targetSymbols.mapNotNull { it.containingFile }.toTypedArray()
            ),
        )
    }

}
