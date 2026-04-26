@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target

import com.github.klee0kai.crossbox.core.CrossboxModel
import com.github.klee0kai.crossbox.core.FieldInfo
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
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CrossboxModelProcessor : TargetSymbolProcessor {

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
                    name = "crossboxFieldList",
                    type = List::class.asClassName()
                        .parameterizedBy(
                            FieldInfo::class.asTypeName()
                                .parameterizedBy(classDeclaration.toClassName())
                        ),
                ) {
                    receiver(classDeclaration.toClassName().nestedClass("Companion"))


                    genGetter {
                        addCode("return listOf(\n")
                        primaryConstructorTypes
                            .forEach { prop ->
                                val propRawType = prop.type.resolve().toTypeName().rawType().copy(nullable = false)

                                val getLambdaBody = prop.name?.asString() ?: ""
                                val setLambdaBody = when {
                                    prop.isVar && prop.type.resolve().isMarkedNullable -> {
                                        CodeBlock.of(
                                            "%L = it as? %T",
                                            prop.name?.asString(),
                                            prop.type.resolve().toTypeName()
                                        )
                                    }

                                    prop.isVar -> CodeBlock.of(
                                        "%L = it as %T",
                                        prop.name?.asString(),
                                        prop.type.resolve().toTypeName()
                                    )

                                    else -> CodeBlock.of("")
                                }

                                addCode(
                                    "  %T(name = %S, kclass = %T::class, annotations = listOf(%L), getValue = { %L }, setValue = { %L }),\n",
                                    FieldInfo::class.asTypeName(),
                                    prop.name?.asString(),
                                    propRawType,
                                    prop.annotations.joinToString {
                                        it.toAnnotationSpec().toString().removePrefix("@")
                                    },
                                    getLambdaBody,
                                    setLambdaBody,
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
                                "  %L = %L ?: pair?.%L,\n",
                                prop.name?.asString(),
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
                            val propAnn = prop.type.resolve()
                                .declaration.getAnnotationsByType(CrossboxModel::class)
                                .firstOrNull()
                            if (propAnn?.merge == true) {
                                addCode(
                                    "  %L = %L?.deepMerge(pair?.%L) ?: pair?.%L,\n",
                                    prop.name?.asString(),
                                    prop.name?.asString(),
                                    prop.name?.asString(),
                                    prop.name?.asString(),
                                )
                            } else {
                                addCode(
                                    "  %L = %L ?: pair?.%L,\n",
                                    prop.name?.asString(),
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
                                "if (%L != null && %L != changed.%L)",
                                lambdaName,
                                prop.name?.asString() ?: "",
                                prop.name?.asString() ?: ""
                            )
                            addStatement("%L()", lambdaName)

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

fun TypeName.rawType(): TypeName {
    val typeName = this
    if (typeName is ParameterizedTypeName) {
        return typeName.rawType
    }
    if (typeName is WildcardTypeName) {
        if (!typeName.outTypes.isEmpty()) return typeName.outTypes.first().rawType()
    }
    return typeName.copy(nullable = false)
}