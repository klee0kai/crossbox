@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target

import com.github.klee0kai.crossbox.core.CrossboxAsyncInterface
import com.github.klee0kai.crossbox.core.CrossboxGenInterface
import com.github.klee0kai.crossbox.processor.ksp.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.TargetFileProcessor
import com.github.klee0kai.crossbox.processor.poet.*
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

class CrossboxAsyncInterfaceProcessor : TargetFileProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxAsyncInterface::class.asClassName().canonicalName)
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

        val asyncInterfaceAnn = classDeclaration.getAnnotationsByType(CrossboxAsyncInterface::class)
            .firstOrNull() ?: return null

        val crossboxGenInterfaceAnn = classDeclaration.getAnnotationsByType(CrossboxGenInterface::class)
            .firstOrNull()

        if (crossboxGenInterfaceAnn != null) {
            // Processing the annotation of the generated interface
            return null
        }

        val asyncInterfaceClName = ClassName(
            fileOwner.packageName.asString().crossboxPackageName,
            "${classDeclaration.simpleName.getShortName()}Async"
        )

        val toAsyncClName = ClassName(
            fileOwner.packageName.asString().crossboxPackageName,
            "${classDeclaration.simpleName.getShortName()}ToAsync"
        )

        val fromAsyncClName = ClassName(
            fileOwner.packageName.asString().crossboxPackageName,
            "${classDeclaration.simpleName.getShortName()}FromAsync"
        )


        val fileSpec = genFileSpec(asyncInterfaceClName.packageName, asyncInterfaceClName.simpleName) {
            genLibComment()
            addImport("kotlinx.coroutines", "async", "launch")

            genInterface(asyncInterfaceClName) {
                if (asyncInterfaceAnn.genProperties) {
                    classDeclaration.getDeclaredProperties()
                        .filter { it.isPublic() }
                        .forEach { property ->
                            genProperty(
                                property.simpleName.asString(),
                                property.type.resolve().toClassName(),
                            ) {
                                mutable(property.isMutable)
                            }
                        }
                }

                if (asyncInterfaceAnn.genFunctions) {
                    classDeclaration
                        .getDeclaredFunctions()
                        .filter { !it.isConstructor() && it.isPublic() }
                        .forEach { function ->
                            genFun(function.simpleName.asString()) {
                                declareSameParameters(function)
                                addModifiers(KModifier.ABSTRACT)
                                function.asyncReturnType?.let { returns(it) }
                            }
                        }
                }
            }

            genClass(toAsyncClName) {
                addModifiers(KModifier.OPEN)
                addSuperinterface(asyncInterfaceClName)

                genPrimaryConstructor {
                    addParameter("crossboxOrigin", classDeclaration.toClassName())
                    addParameter("crossboxScope", CoroutineScope::class.asClassName())
                }
                genProperty(name = "crossboxOrigin", classDeclaration.toClassName()) {
                    initFromConstructor()
                }
                genProperty(name = "crossboxScope", CoroutineScope::class.asClassName()) {
                    initFromConstructor()
                }

                if (asyncInterfaceAnn.genProperties) {
                    classDeclaration.getDeclaredProperties()
                        .filter { it.isPublic() }
                        .forEach { property ->
                            genProxyProperty(
                                originName = "crossboxOrigin",
                                property = property,
                            )
                        }
                }

                if (asyncInterfaceAnn.genFunctions) {
                    classDeclaration
                        .getDeclaredFunctions()
                        .filter { !it.isConstructor() && it.isPublic() }
                        .forEach { function ->
                            genFun(function.simpleName.asString()) {
                                declareSameParameters(function)
                                addModifiers(KModifier.OVERRIDE)
                                val proxyInvokeCode = CodeBlock.builder().apply {
                                    beginControlFlow("with (crossboxOrigin)")
                                    addStatement(
                                        " %L( %L ) ",
                                        function.simpleName.asString(),
                                        parameters.joinToString { arg ->
                                            if (arg.modifiers.contains(KModifier.VARARG)) "*${arg.name}"
                                            else arg.name
                                        },
                                    )
                                    endControlFlow()
                                }.build()

                                val returnType = function.returnType?.resolve()?.toClassName()
                                when {
                                    returnType != null && !function.isSuspend -> {
                                        returns(returnType)
                                        addCode("return ")
                                        addCode(proxyInvokeCode)
                                    }

                                    returnType != null && returnType != Unit::class.asClassName() -> {
                                        returns(Deferred::class.asClassName().parameterizedBy(returnType))
                                        beginControlFlow("return crossboxScope.async")
                                        addCode(proxyInvokeCode)
                                        endControlFlow()
                                    }

                                    function.isSuspend -> {
                                        returns(Job::class.asClassName())
                                        beginControlFlow("return crossboxScope.launch")
                                        addCode(proxyInvokeCode)
                                        endControlFlow()
                                    }
                                }
                            }
                        }
                }
            }



            genClass(fromAsyncClName) {
                addModifiers(KModifier.OPEN)
                addSuperinterface(classDeclaration.toClassName())

                genPrimaryConstructor {
                    addParameter("crossboxOrigin", asyncInterfaceClName)
                }
                genProperty(name = "crossboxOrigin", asyncInterfaceClName) {
                    initFromConstructor()
                }

                if (asyncInterfaceAnn.genProperties) {
                    classDeclaration.getDeclaredProperties()
                        .filter { it.isPublic() }
                        .forEach { property ->
                            genProxyProperty(
                                originName = "crossboxOrigin",
                                property = property,
                            )
                        }
                }

                if (asyncInterfaceAnn.genFunctions) {
                    classDeclaration
                        .getDeclaredFunctions()
                        .filter { !it.isConstructor() && it.isPublic() }
                        .forEach { function ->
                            genFun(function.simpleName.asString()) {
                                declareSameParameters(function)
                                addModifiers(KModifier.OVERRIDE)
                                if (function.isSuspend) addModifiers(KModifier.SUSPEND)


                                val proxyInvokeCode = CodeBlock.builder().apply {
                                    beginControlFlow("with (crossboxOrigin)")
                                    addStatement(
                                        " %L( %L ) ",
                                        function.simpleName.asString(),
                                        parameters.joinToString { arg ->
                                            if (arg.modifiers.contains(KModifier.VARARG)) "*${arg.name}"
                                            else arg.name
                                        },
                                    )
                                    endControlFlow()
                                }.build()

                                val returnType = function.returnType?.resolve()?.toClassName()
                                when {
                                    returnType != null && !function.isSuspend -> {
                                        addCode("return ")
                                        addCode(proxyInvokeCode)
                                    }

                                    returnType != null && returnType != Unit::class.asClassName() -> {
                                        addCode("return ")
                                        addCode(proxyInvokeCode)
                                        addCode(".await()")
                                    }

                                    function.isSuspend -> {
                                        addCode("return ")
                                        addCode(proxyInvokeCode)
                                        addCode(".join()")
                                    }
                                }
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