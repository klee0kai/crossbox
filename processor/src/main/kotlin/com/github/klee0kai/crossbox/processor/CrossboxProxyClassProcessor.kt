@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.core.CrossboxGenInterface
import com.github.klee0kai.crossbox.core.CrossboxProxyClass
import com.github.klee0kai.crossbox.core.InvokeFunctionEvent
import com.github.klee0kai.crossbox.core.InvokeFunctionProcessor
import com.github.klee0kai.crossbox.processor.ksp.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.TargetFileProcessor
import com.github.klee0kai.crossbox.processor.poet.*
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CrossboxProxyClassProcessor : TargetFileProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxProxyClass::class.asClassName().canonicalName)
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
        if (classDeclaration.classKind != ClassKind.INTERFACE) return null

        val crossboxProxyAnn = classDeclaration.getAnnotationsByType(CrossboxProxyClass::class)
            .firstOrNull() ?: return null

        val crossboxGenInterfaceAnn = classDeclaration.getAnnotationsByType(CrossboxGenInterface::class)
            .firstOrNull()

        if (crossboxGenInterfaceAnn != null) {
            // Processing the annotation of the generated interface
            return null
        }

        val genClassName = ClassName(
            fileOwner.packageName.asString().crossboxPackageName,
            "${classDeclaration.simpleName.getShortName()}CrossboxProxy"
        )
        val fileSpec = genFileSpec(genClassName.packageName, genClassName.simpleName) {
            genLibComment()

            genClass(genClassName) {
                addModifiers(KModifier.OPEN)
                addSuperinterface(classDeclaration.toClassName())

                genPrimaryConstructor {
                    addParameter(
                        name = "crossboxOrigin",
                        classDeclaration.toClassName()
                    )

                    addParameter(
                        ParameterSpec.builder(
                            name = "crossBoxProxyProcessor",
                            InvokeFunctionProcessor::class.asClassName(),
                        ).defaultValue("object : InvokeFunctionProcessor {}").build()
                    )
                }

                genProperty(
                    name = "crossboxOrigin",
                    classDeclaration.toClassName(),
                ) {
                    initFromConstructor()
                }

                genProperty(
                    name = "crossBoxProxyProcessor",
                    InvokeFunctionProcessor::class.asClassName(),
                ) {
                    initFromConstructor()
                }

                if (crossboxProxyAnn.genProperties) {
                    classDeclaration.getDeclaredProperties()
                        .filter { it.isPublic() }
                        .forEach { property ->
                            genProperty(
                                property.simpleName.asString(),
                                property.type.resolve().toClassName(),
                            ) {
                                addModifiers(KModifier.OVERRIDE)

                                genGetter {
                                    addStatement(
                                        "val event = %T( %S )",
                                        InvokeFunctionEvent::class,
                                        "getter_${property.simpleName.asString()}",
                                    )
                                    addStatement("val endProcessor = crossBoxProxyProcessor.startFunction(event)")
                                    beginControlFlow("try")
                                    addStatement("return crossboxOrigin.%L", property.simpleName.asString())
                                    endControlFlow()
                                    beginControlFlow("finally")
                                    addStatement("endProcessor?.invoke(event)")
                                    endControlFlow()
                                }

                                if (property.isMutable) {
                                    mutable(property.isMutable)
                                    genSetter {
                                        addParameter("newValue", property.type.resolve().toClassName())
                                        addStatement(
                                            "val event = %T( %S, listOf(newValue) )",
                                            InvokeFunctionEvent::class,
                                            "setter_${property.simpleName.asString()}",
                                        )
                                        addStatement("val endProcessor = crossBoxProxyProcessor.startFunction(event)")
                                        beginControlFlow("try")
                                        addStatement("crossboxOrigin.%L = newValue", property.simpleName.asString())
                                        endControlFlow()
                                        beginControlFlow("finally")
                                        addStatement("endProcessor?.invoke(event)")
                                        endControlFlow()
                                    }
                                }
                            }
                        }
                }

                if (crossboxProxyAnn.genFunctions) {
                    classDeclaration
                        .getDeclaredFunctions()
                        .filter { !it.isConstructor() && it.isPublic() }
                        .forEach { function ->
                            genFun(function.simpleName.asString()) {
                                addModifiers(KModifier.OVERRIDE)
                                function.returnType?.resolve()?.toClassName()?.let { returns(it) }
                                function.extensionReceiver?.resolve()?.toClassName()?.let { receiver(it) }
                                if (function.modifiers.contains(Modifier.SUSPEND)) addModifiers(KModifier.SUSPEND)

                                function.parameters.forEach { param ->
                                    addParameter(
                                        ParameterSpec.builder(
                                            name = param.name?.asString() ?: "",
                                            type = param.type.resolve().toTypeName(),
                                        ).apply {
                                            if (param.isVararg) addModifiers(KModifier.VARARG)
                                        }.build()
                                    )
                                }

                                addStatement(
                                    "val event = %T( %S , listOf ( %L ) )",
                                    InvokeFunctionEvent::class,
                                    function.simpleName.asString(),
                                    parameters.joinToString { arg -> arg.name },
                                )
                                addStatement("val endProcessor = crossBoxProxyProcessor.startFunction(event)")
                                beginControlFlow("try")
                                beginControlFlow("with (crossboxOrigin)")
                                addStatement(
                                    "return %L( %L )",
                                    function.simpleName.asString(),
                                    parameters.joinToString { arg ->
                                        if (arg.modifiers.contains(KModifier.VARARG)) "*${arg.name}"
                                        else arg.name
                                    },
                                )
                                endControlFlow()
                                endControlFlow()
                                beginControlFlow("finally")
                                addStatement("endProcessor?.invoke(event)")
                                endControlFlow()
                            }
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