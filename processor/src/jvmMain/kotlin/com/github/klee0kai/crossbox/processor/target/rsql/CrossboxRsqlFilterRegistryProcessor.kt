@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target.rsql

import com.github.klee0kai.crossbox.core.CrossboxRsqlFilter
import com.github.klee0kai.crossbox.processor.common.findCommonPgk
import com.github.klee0kai.crossbox.processor.exceptions.forEachKsNode
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
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

class CrossboxRsqlFilterRegistryProcessor : TargetSymbolProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxRsqlFilter::class.asClassName().canonicalName)
            .filter { it.getAnnotationsByType(CrossboxRsqlFilter::class).firstOrNull()?.commonRegistry == true }

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
        val rsqlRegistryClName = ClassName(commonPkg.crossboxPackageName, "RsqlFilterRegistry")
        val rsqlFilterToolClName = ClassName(commonPkg.crossboxPackageName + ".RsqlFilterRegistry", "RsqlFilterTool")

        // Generate registry file with all collected rsql filter classes
        val fileSpec = genFileSpec(rsqlRegistryClName.packageName, rsqlRegistryClName.simpleName) {
            genLibComment()

            genObject(rsqlRegistryClName) {

                genClass(rsqlFilterToolClName) {
                    val tType = TypeVariableName("T", Any::class.asClassName())
                    addTypeVariable(tType)
                    addModifiers(KModifier.OPEN)
                    val iterableFilterType = LambdaTypeName.get(
                        receiver = Iterable::class.asClassName().parameterizedBy(tType),
                        parameters = listOf(ParameterSpec("rsqlQuery", String::class.asClassName())),
                        returnType = List::class.asClassName().parameterizedBy(tType),
                    )
                    genPrimaryConstructor {
                        addParameter("type", KClass::class.asClassName().parameterizedBy(tType))
                        addParameter("filterByRsqlQuery", iterableFilterType)
                    }

                    genProperty(
                        name = "type",
                        type = KClass::class.asClassName().parameterizedBy(tType),
                    ) {
                        initFromConstructor()
                    }
                    genProperty(
                        name = "filterByRsqlQuery",
                        type = iterableFilterType,
                    ) {
                        initFromConstructor()
                    }
                }

                // Generate property with map of rsql filters
                genProperty(
                    name = "rsqlFilterRegistry",
                    type = Map::class.asClassName()
                        .parameterizedBy(
                            KClass::class.asClassName().parameterizedBy(STAR),
                            rsqlFilterToolClName.parameterizedBy(STAR)
                        ),
                    KModifier.PUBLIC
                ) {
                    genGetter {
                        addCode("return mapOf(\n")
                        targetSymbols.forEachKsNode { index, classDecl ->
                            val className = (classDecl as KSClassDeclaration).toClassName()
                            addCode(
                                "%T::class to RsqlFilterTool(type = %T::class, filterByRsqlQuery = { rsql -> this.%M(rsql) } ),\n",
                                className,
                                className,
                                MemberName(className.packageName.crossboxPackageName, "filterByRsqlQuery"),
                            )
                        }
                        addCode(")")
                    }
                }

                genFun("filterByRsqlQuery") {
                    val tType = TypeVariableName("T").copy(reified = true)
                    val iterableOfT = Iterable::class.asClassName().parameterizedBy(TypeVariableName("T"))
                    val lambdaType = LambdaTypeName.get(
                        receiver = iterableOfT,
                        parameters = listOf(ParameterSpec("rsqlQuery", String::class.asClassName())),
                        returnType = List::class.asClassName().parameterizedBy(TypeVariableName("T"))
                    )
                    addModifiers(KModifier.INLINE)
                    addTypeVariable(tType)
                    addParameter("value", iterableOfT)
                    addParameter("rsqlQuery", String::class.asClassName())
                    returns(List::class.asClassName().parameterizedBy(TypeVariableName("T")))
                    addStatement(
                        "val filterLambda = rsqlFilterRegistry[T::class]!!.filterByRsqlQuery as %T",
                        lambdaType
                    )
                    addStatement("return filterLambda.invoke(value, rsqlQuery)")
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
