@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target.table

import com.github.klee0kai.crossbox.core.CrossboxJoineryDataFrame
import com.github.klee0kai.crossbox.processor.common.findCommonPgk
import com.github.klee0kai.crossbox.processor.exceptions.forEachKsNode
import com.github.klee0kai.crossbox.processor.ksp.arch.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.arch.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.arch.TargetSymbolProcessor
import com.github.klee0kai.crossbox.processor.poet.*
import com.github.klee0kai.crossbox.processor.target.table.CrossboxJoineryDataFrameProcessor.Companion.dataFrameClazz
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
import kotlin.reflect.KClass

class CrossboxJoineryRegistryProcessor : TargetSymbolProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxJoineryDataFrame::class.asClassName().canonicalName)
            .filter { it.getAnnotationsByType(CrossboxJoineryDataFrame::class).firstOrNull()?.commonRegistry == true }
            .groupBy { it.validate() }

        return SymbolsToProcess(
            symbolsForProcessing = annotatedSymbols[true].orEmpty(),
            symbolsForReprocessing = annotatedSymbols[false].orEmpty(),
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
        val joineryRegistryClName = ClassName(commonPkg.crossboxPackageName, "JoineryRegistry")
        val joineryToolClName = ClassName(commonPkg.crossboxPackageName + ".JoineryRegistry", "JoineryTool")

        // Generate registry file with all collected serializable classes
        val fileSpec = genFileSpec(joineryRegistryClName.packageName, joineryRegistryClName.simpleName) {
            genLibComment()

            genObject(joineryRegistryClName) {

                genClass(joineryToolClName) {
                    val tType = TypeVariableName("T", Any::class.asClassName())
                    addTypeVariable(tType)
                    addModifiers(KModifier.OPEN)
                    val iterableToDataFrameType = LambdaTypeName.get(
                        receiver = Iterable::class.asClassName().parameterizedBy(tType),
                        parameters = emptyList(),
                        returnType = CrossboxJoineryDataFrameProcessor.dataFrameClazz,
                    )
                    genPrimaryConstructor {
                        addParameter("type", KClass::class.asClassName().parameterizedBy(tType))
                        addParameter("iterableToDataFrame", iterableToDataFrameType)
                    }

                    genProperty(
                        name = "type",
                        type = KClass::class.asClassName().parameterizedBy(tType),
                    ) {
                        initFromConstructor()
                    }
                    genProperty(
                        name = "iterableToDataFrame",
                        type = iterableToDataFrameType,
                    ) {
                        initFromConstructor()
                    }
                }

                // Generate property with map of serializers
                genProperty(
                    name = "joineryRegistry",
                    type = Map::class.asClassName()
                        .parameterizedBy(
                            KClass::class.asClassName().parameterizedBy(STAR),
                            joineryToolClName.parameterizedBy(STAR)
                        ),
                    KModifier.PUBLIC
                ) {
                    genGetter {
                        addCode("return mapOf(\n")
                        targetSymbols.forEachKsNode { index, classDecl ->
                            val className = (classDecl as KSClassDeclaration).toClassName()
                            addCode(
                                "%T::class to JoineryTool(type = %T::class, iterableToDataFrame = { %M() } ),\n",
                                className,
                                className,
                                MemberName(className.packageName.crossboxPackageName, "toDataFrame"),
                            )
                        }
                        addCode(")")
                    }
                }

                genFun("toDataFrame") {
                    val tType = TypeVariableName("T").copy(reified = true)
                    val iterableOfT = Iterable::class.asClassName().parameterizedBy(TypeVariableName("T"))
                    val lambdaType = LambdaTypeName.get(
                        receiver = iterableOfT,
                        returnType = dataFrameClazz
                    )
                    addModifiers(KModifier.INLINE)
                    addTypeVariable(tType)
                    addParameter("value", iterableOfT)
                    returns(dataFrameClazz)
                    addStatement(
                        "val transformLambda = joineryRegistry[T::class]!!.iterableToDataFrame as %T",
                        lambdaType
                    )
                    addStatement("return transformLambda.invoke(value)")
                }

                genFun("toDataFrame") {
                    val tType = TypeVariableName("T").copy(reified = true)
                    val iterableOfT = Iterable::class.asClassName().parameterizedBy(TypeVariableName("T"))
                    val lambdaType = LambdaTypeName.get(
                        receiver = iterableOfT,
                        returnType = dataFrameClazz
                    )
                    addModifiers(KModifier.INLINE)
                    addTypeVariable(tType)
                    addParameter("value", tType)
                    returns(dataFrameClazz)
                    addStatement(
                        "val transformLambda = joineryRegistry[T::class]!!.iterableToDataFrame as %T",
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