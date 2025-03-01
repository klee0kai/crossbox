@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.core.CrossboxModel
import com.github.klee0kai.crossbox.core.FieldInfo
import com.github.klee0kai.crossbox.processor.ksp.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.TargetFileProcessor
import com.github.klee0kai.crossbox.processor.poet.*
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName

class CrossboxModelProcessor : TargetFileProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxModel::class.asClassName().canonicalName)
            .groupBy { it.validate() }

        return SymbolsToProcess(
            symbolsForProcessing = annotatedSymbols[true].orEmpty(),
            symbolsForReprocessing = annotatedSymbols[false].orEmpty(),
        )

    }

    override suspend fun process(
        validSymbol: KSAnnotated,
        resolver: Resolver,
        options: Map<String, String>,
        logger: KSPLogger
    ): GenSpec? {
        val fileOwner = validSymbol.containingFile ?: return null
        val classDeclaration = validSymbol as? KSClassDeclaration ?: return null
        val primaryConstructorTypes = classDeclaration.primaryConstructor
            ?.parameters ?: return null
        val crossboxModelAnn = classDeclaration.getAnnotationsByType(CrossboxModel::class)
            .firstOrNull() ?: return null

        val fileSpec = genFileSpec(
            packageName = fileOwner.packageName.asString().crossboxPackageName,
            fileName = "${classDeclaration.simpleName.getShortName()}CrossboxExt"
        ) {
            genLibComment()

            if (crossboxModelAnn.fieldList) {
                genProperty(
                    name = "fieldList",
                    type = List::class.asClassName()
                        .parameterizedBy(FieldInfo::class.asTypeName()),
                ) {
                    receiver(classDeclaration.toClassName().nestedClass("Companion"))
                    genGetter {
                        addCode("return listOf(\n")
                        primaryConstructorTypes
                            .forEach { prop ->
                                addCode(
                                    "%T( %S , %T::class ),\n",
                                    FieldInfo::class.asTypeName(),
                                    prop.name?.asString(),
                                    prop.type.resolve().toClassName().copy(nullable = false),
                                )
                            }
                        addCode(")")
                    }
                }
            }

            if (crossboxModelAnn.merge) {
                genFun(name = "merge") {
                    receiver(classDeclaration.toClassName())
                    returns(classDeclaration.toClassName())
                    addParameter("pair", classDeclaration.toClassName().copy(nullable = true))
                    addCode("return %T(\n", classDeclaration.toClassName())
                    primaryConstructorTypes
                        .forEach { prop ->
                            addCode(
                                " %L ?: pair?.%L,\n",
                                prop.name?.asString(),
                                prop.name?.asString()
                            )
                        }
                    addCode(")")
                }

                genFun(name = "deepMerge") {
                    receiver(classDeclaration.toClassName())
                    returns(classDeclaration.toClassName())
                    addParameter("pair", classDeclaration.toClassName().copy(nullable = true))
                    addCode("return %T(\n", classDeclaration.toClassName())
                    primaryConstructorTypes
                        .forEach { prop ->
                            val propAnn =
                                prop.type.resolve().declaration.getAnnotationsByType(CrossboxModel::class)
                                    .firstOrNull()
                            if (propAnn?.merge == true) {
                                addCode(
                                    " %L?.deepMerge( pair?.%L ) ?: pair?.%L,\n",
                                    prop.name?.asString(),
                                    prop.name?.asString(),
                                    prop.name?.asString(),
                                )
                            } else {
                                addCode(
                                    " %L ?: pair?.%L,\n",
                                    prop.name?.asString(),
                                    prop.name?.asString()
                                )
                            }
                        }
                    addCode(")")
                }
            }

            if (crossboxModelAnn.changes) {
                genFun(name = "changes") {
                    receiver(classDeclaration.toClassName())
                    addParameter("changed", classDeclaration.toClassName())
                    primaryConstructorTypes
                        .forEach { prop ->
                            val lambdaName = "${prop.name?.asString()}Changed"
                            addParameter(
                                ParameterSpec.builder(
                                    name = lambdaName,
                                    type = LambdaTypeName.get(null, emptyList(), UNIT).copy(nullable = true)
                                ).defaultValue("null").build()
                            )

                            beginControlFlow(
                                "if (%L != null && %L != changed.%L ) ",
                                lambdaName,
                                prop.name?.asString() ?: "",
                                prop.name?.asString() ?: ""
                            )
                            addCode("%L()", lambdaName)

                            endControlFlow()
                        }

                }
            }
        }

        return GenSpec(
            fileSpec = fileSpec,
            // https://kotlinlang.org/docs/ksp-incremental.html
            dependencies = Dependencies(aggregating = false, fileOwner),
        )

    }
}