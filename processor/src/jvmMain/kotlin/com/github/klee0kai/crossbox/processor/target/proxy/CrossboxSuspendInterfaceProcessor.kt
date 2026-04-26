@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target.proxy

import com.github.klee0kai.crossbox.core.CrossboxGenInterface
import com.github.klee0kai.crossbox.core.CrossboxSuspendInterface
import com.github.klee0kai.crossbox.processor.ksp.arch.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.arch.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.arch.TargetSymbolProcessor
import com.github.klee0kai.crossbox.processor.poet.*
import com.github.klee0kai.crossbox.processor.poet.declareSameParameters
import com.github.klee0kai.crossbox.processor.poet.genProxyProperty
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

class CrossboxSuspendInterfaceProcessor : TargetSymbolProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxSuspendInterface::class.asClassName().canonicalName)
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

        val suspendInterfaceAnn = classDeclaration.getAnnotationsByType(CrossboxSuspendInterface::class)
            .firstOrNull() ?: return null

        val crossboxGenInterfaceAnn = classDeclaration.getAnnotationsByType(CrossboxGenInterface::class)
            .firstOrNull()

        if (crossboxGenInterfaceAnn != null) {
            // Processing the annotation of the generated interface
            return null
        }

        val suspendInterfaceClName = ClassName(
            fileOwner.packageName.asString().crossboxPackageName,
            "${classDeclaration.simpleName.getShortName()}Suspend"
        )

        val toSuspendAdapterClName = ClassName(
            fileOwner.packageName.asString().crossboxPackageName,
            "${classDeclaration.simpleName.getShortName()}ToSuspend"
        )

        val fileSpec = genFileSpec(suspendInterfaceClName.packageName, suspendInterfaceClName.simpleName) {
            genLibComment()

            genInterface(suspendInterfaceClName) {
                if (suspendInterfaceAnn.genProperties) {
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

                if (suspendInterfaceAnn.genFunctions) {
                    classDeclaration
                        .getDeclaredFunctions()
                        .filter { !it.isConstructor() && it.isPublic() }
                        .forEach { function ->
                            genFun(function.simpleName.asString()) {
                                declareSameParameters(function)
                                addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                            }
                        }
                }
            }



            genClass(toSuspendAdapterClName) {
                addModifiers(KModifier.OPEN)
                addSuperinterface(suspendInterfaceClName)

                genPrimaryConstructor {
                    addParameter(
                        name = "crossboxOrigin",
                        classDeclaration.toClassName()
                    )
                }

                genProperty(
                    name = "crossboxOrigin",
                    classDeclaration.toClassName(),
                ) {
                    initFromConstructor()
                }


                if (suspendInterfaceAnn.genProperties) {
                    classDeclaration.getDeclaredProperties()
                        .filter { it.isPublic() }
                        .forEach { property ->
                            genProxyProperty(
                                originName = "crossboxOrigin",
                                property = property,
                            )
                        }
                }

                if (suspendInterfaceAnn.genFunctions) {
                    classDeclaration
                        .getDeclaredFunctions()
                        .filter { !it.isConstructor() && it.isPublic() }
                        .forEach { function ->
                            genFun(function.simpleName.asString()) {
                                declareSameParameters(function)
                                addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)

                                beginControlFlow("return with (crossboxOrigin)")
                                addStatement(
                                    " %L( %L )",
                                    function.simpleName.asString(),
                                    parameters.joinToString { arg ->
                                        if (arg.modifiers.contains(KModifier.VARARG)) "*${arg.name}"
                                        else arg.name
                                    },
                                )
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