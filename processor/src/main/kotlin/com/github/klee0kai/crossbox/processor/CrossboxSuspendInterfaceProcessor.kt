@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor

import com.github.klee0kai.crossbox.core.CrossboxGenInterface
import com.github.klee0kai.crossbox.core.CrossboxSuspendInterface
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
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CrossboxSuspendInterfaceProcessor : TargetFileProcessor {

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

        val genClassName = ClassName(
            fileOwner.packageName.asString().crossboxPackageName,
            "${classDeclaration.simpleName.getShortName()}Suspend"
        )
        val fileSpec = genFileSpec(genClassName.packageName, genClassName.simpleName) {
            genLibComment()

            genInterface(genClassName) {
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
                                addModifiers(KModifier.ABSTRACT)
                                addModifiers(KModifier.SUSPEND)
                                function.returnType?.resolve()?.toClassName()?.let { returns(it) }
                                function.extensionReceiver?.resolve()?.toClassName()?.let { receiver(it) }

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