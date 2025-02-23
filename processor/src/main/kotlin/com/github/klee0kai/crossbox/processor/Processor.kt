@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.core.Crossbox
import com.github.klee0kai.crossbox.core.FieldInfo
import com.github.klee0kai.crossbox.processor.poet.*
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class Processor(
    private val options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    companion object {
        val PROJECT_URL = "https://github.com/klee0kai/crossbox"
    }

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
                val primaryConstructorTypes = classDeclaration.primaryConstructor
                    ?.parameters ?: return@launch
                val crossboxAnn = classDeclaration.getAnnotationsByType(Crossbox::class).firstOrNull() ?: return@launch

                codeGenerator.genKtFile(
                    // https://kotlinlang.org/docs/ksp-incremental.html
                    dependencies = Dependencies(aggregating = false, fileOwner),
                    packageName = "${fileOwner.packageName.asString()}.crossbox",
                    fileName = "${classDeclaration.simpleName.getShortName()}CrossboxExt"
                ) {
                    genLibComment()

                    if (crossboxAnn.fieldList) {
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
                                            prop.type.resolve().toClassName(),
                                        )
                                    }
                                addCode(")")
                            }
                        }
                    }

                    if (crossboxAnn.merge) {
                        genFun(name = "merge") {
                            receiver(classDeclaration.toClassName())
                            returns(classDeclaration.toClassName())
                            addParameter("pair", classDeclaration.toClassName())

                        }
                    }

                    if (crossboxAnn.changes) {
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


                            classDeclaration
                                .getAllProperties()
                                .forEach { prop ->

                                }

                        }
                    }

                }
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