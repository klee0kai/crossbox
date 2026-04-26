@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target.table

import com.github.klee0kai.crossbox.core.CrossboxJoineryDataFrame
import com.github.klee0kai.crossbox.core.CrossboxModel
import com.github.klee0kai.crossbox.processor.ksp.arch.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.arch.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.arch.TargetSymbolProcessor
import com.github.klee0kai.crossbox.processor.poet.crossboxPackageName
import com.github.klee0kai.crossbox.processor.poet.genFileSpec
import com.github.klee0kai.crossbox.processor.poet.genFun
import com.github.klee0kai.crossbox.processor.poet.genLibComment
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CrossboxJoineryDataFrameProcessor : TargetSymbolProcessor {

    companion object {
        val dataFrameClazz = ClassName("joinery", "DataFrame")
            .parameterizedBy(Any::class.asClassName().copy(nullable = true))
    }

    data class ExpandedProperty(
        val originalName: String,
        val columnName: String,
        val fullPath: String,
        val type: KSType,
        val isFromNestedModel: Boolean = false,
    )

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxJoineryDataFrame::class.asClassName().canonicalName)
            .groupBy { it.validate() }

        return SymbolsToProcess(
            symbolsForProcessing = annotatedSymbols[true].orEmpty(),
            symbolsForReprocessing = annotatedSymbols[false].orEmpty(),
        )

    }

    override suspend fun process(
        targetSymbol: KSAnnotated,
        resolver: Resolver,
        options: Map<String, String>,
        logger: KSPLogger
    ): GenSpec? {
        val fileOwner = targetSymbol.containingFile ?: return null
        val classDeclaration = targetSymbol as? KSClassDeclaration ?: return null

        classDeclaration.getAnnotationsByType(CrossboxJoineryDataFrame::class)
            .firstOrNull() ?: return null

        val modelClassName = classDeclaration.toClassName()
        val packageName = fileOwner.packageName.asString().crossboxPackageName

        val joineryClName = ClassName(packageName, "${classDeclaration.simpleName.getShortName()}JoineryDataFrame")

        val properties = classDeclaration.getDeclaredProperties()
            .filter { it.isPublic() }
            .toList()

        // Flatten nested models and handle lists
        val flatProperties = mutableListOf<ExpandedProperty>()
        properties.forEach { prop ->
            expandPropertyForJoinery(prop, resolver, flatProperties)
        }

        val fileSpec = genFileSpec(packageName, joineryClName.simpleName) {
            genLibComment()

            val listClazz = ClassName("java.util", "ArrayList")

            // Function to convert Iterable<Model> to DataFrame
            genFun("toDataFrame") {
                receiver(
                    Iterable::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                returns(dataFrameClazz)

                // Create column data structure: Map<String, MutableList<Any?>>
                addStatement(
                    "val columnData = mutableMapOf<%T, %T>()",
                    String::class.asClassName(),
                    List::class.asClassName().parameterizedBy(Any::class.asClassName().copy(nullable = true))
                )

                // Initialize each column list
                flatProperties.forEach { prop ->
                    addStatement("columnData[\"${prop.columnName}\"] = %T()", listClazz)
                }

                // Populate columns by iterating through items
                beginControlFlow("this.forEach { item ->")
                flatProperties.forEach { expandedProp ->
                    val valueCode = generateValueCode(expandedProp)
                    addStatement("(columnData[\"${expandedProp.columnName}\"] as %T).add($valueCode)", listClazz)
                }
                endControlFlow()

                // Convert map to list of column lists
                addStatement(
                    "val data = listOf(%L)",
                    flatProperties.joinToString(", ") { "columnData[\"${it.columnName}\"] ?: emptyList<Any?>()" })

                // Create DataFrame using Joinery API
                addStatement("val columns = listOf(%L)", flatProperties.joinToString(", ") { "\"${it.columnName}\"" })
                addStatement("return %T(emptyList<Any>(), columns, data)", dataFrameClazz)
            }

            // Function to convert Sequence<Model> to DataFrame
            genFun("toDataFrame") {
                receiver(
                    Sequence::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                returns(dataFrameClazz)

                addStatement("return this.toList().toDataFrame()")
            }

            // Function to convert single item to DataFrame (single row)
            genFun("toDataFrame") {
                receiver(modelClassName)
                returns(dataFrameClazz)

                addStatement("return listOf(this).toDataFrame()")
            }
        }

        return GenSpec(
            fileSpec = fileSpec,
            dependencies = Dependencies(aggregating = false, fileOwner),
        )

    }

    private fun expandPropertyForJoinery(
        property: KSPropertyDeclaration,
        resolver: Resolver,
        result: MutableList<ExpandedProperty>
    ) {
        val propName = property.simpleName.asString()
        val propType = property.type.resolve()
        val nonNullType = propType.makeNotNullable()

        // Check if it's a List type
        if (isListType(nonNullType)) {
            result.add(
                ExpandedProperty(
                    originalName = propName,
                    columnName = propName,
                    fullPath = propName,
                    type = propType,
                    isFromNestedModel = false
                )
            )
        } else if (isNestedModel(nonNullType, resolver)) {
            // For nested models, expand fields with prefix
            val nestedClassDecl = nonNullType.declaration as? KSClassDeclaration
            if (nestedClassDecl != null) {
                nestedClassDecl.getDeclaredProperties()
                    .filter { it.isPublic() }
                    .forEach { nestedProp ->
                        val nestedPropName = nestedProp.simpleName.asString()
                        val nestedPropType = nestedProp.type.resolve()
                        val columnName = "${propName}_${nestedPropName}"

                        result.add(
                            ExpandedProperty(
                                originalName = nestedPropName,
                                columnName = columnName,
                                fullPath = "$propName.$nestedPropName",
                                type = nestedPropType,
                                isFromNestedModel = true
                            )
                        )
                    }
            }
        } else {
            // For simple types, add as is
            result.add(
                ExpandedProperty(
                    originalName = propName,
                    columnName = propName,
                    fullPath = propName,
                    type = propType,
                    isFromNestedModel = false
                )
            )
        }
    }

    private fun generateValueCode(expandedProp: ExpandedProperty): String {
        return if (expandedProp.isFromNestedModel) {
            // Convert address.street to address?.street for safe access
            val parts = expandedProp.fullPath.split(".")
            if (parts.size == 2) {
                "item.${parts[0]}?.${parts[1]}"
            } else {
                "item.${expandedProp.fullPath}"
            }
        } else {
            "item.${expandedProp.fullPath}"
        }
    }

    private fun isListType(type: KSType): Boolean {
        val typeName = type.toTypeName().toString()
        return typeName.startsWith("kotlin.collections.List") ||
                typeName.startsWith("java.util.List") ||
                typeName.startsWith("kotlin.Sequence")
    }

    private fun isNestedModel(type: KSType, resolver: Resolver): Boolean {
        val declaration = type.declaration
        if (declaration !is KSClassDeclaration) return false

        // Check if class has @CrossboxModel annotation
        return declaration.getAnnotationsByType(CrossboxModel::class).firstOrNull() != null
    }

}
