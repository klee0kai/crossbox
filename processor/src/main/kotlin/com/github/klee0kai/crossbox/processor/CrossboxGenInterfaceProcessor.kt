@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor

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
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CrossboxGenInterfaceProcessor : TargetFileProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        // Generate an interface on which the class itself can depend.
        // Validation will never pass at this stage

        return SymbolsToProcess(
            symbolsForProcessing = resolver
                .getSymbolsWithAnnotation(CrossboxGenInterface::class.asClassName().canonicalName)
                .toList(),
            symbolsForReprocessing = emptyList(),
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

        val genClassName = ClassName(
            "${fileOwner.packageName.asString()}.crossbox",
            "I${classDeclaration.simpleName.getShortName()}"
        )
        val fileSpec = genFileSpec(genClassName.packageName, genClassName.simpleName) {
            genLibComment()

            genInterface(genClassName) {
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

                classDeclaration
                    .getDeclaredFunctions()
                    .filter { !it.isConstructor() && it.isPublic() }
                    .forEach { function ->
                        genFun(function.simpleName.asString()) {
                            addModifiers(KModifier.ABSTRACT)
                            function.returnType?.resolve()?.toClassName()?.let { returns(it) }
                            function.extensionReceiver?.resolve()?.toClassName()?.let { receiver(it) }

                            if (function.modifiers.contains(Modifier.SUSPEND)) {
                                addModifiers(KModifier.SUSPEND)
                            }

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

        return GenSpec(
            fileSpec = fileSpec,
            // https://kotlinlang.org/docs/ksp-incremental.html
            dependencies = Dependencies(aggregating = false, fileOwner),
        )

    }
}