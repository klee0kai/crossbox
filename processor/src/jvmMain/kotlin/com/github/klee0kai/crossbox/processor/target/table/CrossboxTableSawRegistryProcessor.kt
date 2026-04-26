@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target.table

import com.github.klee0kai.crossbox.core.CrossboxTableSaw
import com.github.klee0kai.crossbox.processor.common.findCommonPgk
import com.github.klee0kai.crossbox.processor.exceptions.forEachKsNode
import com.github.klee0kai.crossbox.processor.ksp.arch.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.arch.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.arch.TargetSymbolProcessor
import com.github.klee0kai.crossbox.processor.poet.*
import com.github.klee0kai.crossbox.processor.target.table.CrossboxTableSawProcessor.Companion.tableSawClazz
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

class CrossboxTableSawRegistryProcessor : TargetSymbolProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxTableSaw::class.asClassName().canonicalName)
            .filter { it.getAnnotationsByType(CrossboxTableSaw::class).firstOrNull()?.commonRegistry == true }

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
        val tableSawRegistryClName = ClassName(commonPkg.crossboxPackageName, "TableSawRegistry")
        val tableSawToolClName = ClassName(commonPkg.crossboxPackageName + ".TableSawRegistry", "TableSawTool")

        // Generate registry file with all collected serializable classes
        val fileSpec = genFileSpec(tableSawRegistryClName.packageName, tableSawRegistryClName.simpleName) {
            genLibComment()

            genObject(tableSawRegistryClName) {

                genClass(tableSawToolClName) {
                    val tType = TypeVariableName("T", Any::class.asClassName())
                    addTypeVariable(tType)
                    addModifiers(KModifier.OPEN)
                    val iterableToTableSawType = LambdaTypeName.get(
                        receiver = Iterable::class.asClassName().parameterizedBy(tType),
                        parameters = emptyList(),
                        returnType = tableSawClazz,
                    )
                    genPrimaryConstructor {
                        addParameter("type", KClass::class.asClassName().parameterizedBy(tType))
                        addParameter("iterableToTableSaw", iterableToTableSawType)
                    }

                    genProperty(
                        name = "type",
                        type = KClass::class.asClassName().parameterizedBy(tType),
                    ) {
                        initFromConstructor()
                    }
                    genProperty(
                        name = "iterableToTableSaw",
                        type = iterableToTableSawType,
                    ) {
                        initFromConstructor()
                    }
                }

                // Generate property with map of serializers
                genProperty(
                    name = "tableSawRegistry",
                    type = Map::class.asClassName()
                        .parameterizedBy(
                            KClass::class.asClassName().parameterizedBy(STAR),
                            tableSawToolClName.parameterizedBy(STAR)
                        ),
                    KModifier.PUBLIC
                ) {
                    genGetter {
                        addCode("return mapOf(\n")
                        targetSymbols.forEachKsNode { index, classDecl ->
                            val className = (classDecl as KSClassDeclaration).toClassName()
                            addCode(
                                "%T::class to TableSawTool(type = %T::class, iterableToTableSaw = { %M() } ),\n",
                                className,
                                className,
                                MemberName(className.packageName.crossboxPackageName, "toTableSaw"),
                            )
                        }
                        addCode(")")
                    }
                }

                genFun("toTableSaw") {
                    val tType = TypeVariableName("T").copy(reified = true)
                    val iterableOfT = Iterable::class.asClassName().parameterizedBy(TypeVariableName("T"))
                    val lambdaType = LambdaTypeName.get(
                        receiver = iterableOfT,
                        returnType = tableSawClazz
                    )
                    addModifiers(KModifier.INLINE)
                    addTypeVariable(tType)
                    addParameter("value", iterableOfT)
                    returns(tableSawClazz)
                    addStatement(
                        "val transformLambda = tableSawRegistry[T::class]!!.iterableToTableSaw as %T",
                        lambdaType
                    )
                    addStatement("return transformLambda.invoke(value)")
                }

                genFun("toTableSaw") {
                    val tType = TypeVariableName("T").copy(reified = true)
                    val iterableOfT = Iterable::class.asClassName().parameterizedBy(TypeVariableName("T"))
                    val lambdaType = LambdaTypeName.get(
                        receiver = iterableOfT,
                        returnType = tableSawClazz
                    )
                    addModifiers(KModifier.INLINE)
                    addTypeVariable(tType)
                    addParameter("value", tType)
                    returns(tableSawClazz)
                    addStatement(
                        "val transformLambda = tableSawRegistry[T::class]!!.iterableToTableSaw as %T",
                        lambdaType
                    )
                    addStatement("return transformLambda.invoke(arrayListOf(value))")
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
