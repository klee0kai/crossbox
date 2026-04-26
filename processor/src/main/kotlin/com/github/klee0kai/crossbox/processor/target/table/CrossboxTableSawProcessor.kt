@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target.table

import com.github.klee0kai.crossbox.core.CrossboxModel
import com.github.klee0kai.crossbox.core.CrossboxTableSaw
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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CrossboxTableSawProcessor : TargetSymbolProcessor {


    companion object{
        val tableSawClazz = ClassName("tech.tablesaw.api", "Table")
    }

    data class ExpandedProperty(
        val originalName: String,
        val columnName: String,
        val fullPath: String, // For nested access like "address.street"
        val type: KSType,
        val isFromNestedModel: Boolean = false,
    )

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxTableSaw::class.asClassName().canonicalName)
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

        classDeclaration.getAnnotationsByType(CrossboxTableSaw::class)
            .firstOrNull() ?: return null

        val modelClassName = classDeclaration.toClassName()
        val packageName = fileOwner.packageName.asString().crossboxPackageName

        val tableSawClName = ClassName(packageName, "${classDeclaration.simpleName.getShortName()}TableSaw")

        val properties = classDeclaration.getDeclaredProperties()
            .filter { it.isPublic() }
            .toList()

        // Flatten nested models and handle lists
        val flatProperties = mutableListOf<ExpandedProperty>()
        properties.forEach { prop ->
            expandPropertyForTableSaw(prop, resolver, flatProperties)
        }

        val fileSpec = genFileSpec(packageName, tableSawClName.simpleName) {
            genLibComment()


            // Function to convert Iterable<Model> to Table
            genFun("toTableSaw") {
                receiver(
                    Iterable::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                returns(tableSawClazz)

                // Add each flattened property as a column variable
                flatProperties.forEach { expandedProp ->
                    val columnType = getTableSawColumnType(expandedProp.type)
                    addStatement(
                        "val %LColumn = %T.create(\"%L\")",
                        expandedProp.columnName,
                        columnType,
                        expandedProp.columnName
                    )
                }

                beginControlFlow("this.forEach { item ->")
                flatProperties.forEach { expandedProp ->
                    val columnType = getTableSawColumnType(expandedProp.type)
                    generateAppendCodeInline(this, expandedProp, columnType)
                }
                endControlFlow()

                // Create array of columns and create table
                val columnsArrayLines = flatProperties.joinToString(",\n        ") { expandedProp ->
                    "${expandedProp.columnName}Column"
                }
                addCode("return %T.create(\n        %L\n    )\n", tableSawClazz, columnsArrayLines)
            }

            // Function to convert Sequence<Model> to Table
            genFun("toTableSaw") {
                receiver(
                    Sequence::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                returns(tableSawClazz)

                addStatement("return this.toList().toTableSaw()")
            }

            // Function to convert single item to Table (single row)
            genFun("toTableSaw") {
                receiver(modelClassName)
                returns(tableSawClazz)

                addStatement("return listOf(this).toTableSaw()")
            }

            // Function to create empty table with correct schema
            genFun("createEmptyTableSaw") {
                receiver(
                    List::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                returns(tableSawClazz)

                addCode("return %T.create(\n", tableSawClazz)
                flatProperties.forEach { expandedProp ->
                    val columnType = getTableSawColumnType(expandedProp.type)
                    addCode("        %T.create(\"%L\"),\n", columnType, expandedProp.columnName)
                }
                addCode("    )\n")
            }
        }

        return GenSpec(
            fileSpec = fileSpec,
            dependencies = Dependencies(aggregating = false, fileOwner),
        )

    }

    private fun expandPropertyForTableSaw(
        property: KSPropertyDeclaration,
        resolver: Resolver,
        result: MutableList<ExpandedProperty>
    ) {
        val propName = property.simpleName.asString()
        val propType = property.type.resolve()
        val nonNullType = propType.makeNotNullable()
        val typeName = nonNullType.toTypeName().toString()

        // Check if it's a List type
        if (isListType(nonNullType)) {
            // For lists, serialize as JSON string
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

    private fun generateAppendCodeInline(
        builder: FunSpec.Builder,
        expandedProp: ExpandedProperty,
        columnType: ClassName
    ) {
        with(builder) {
            if (expandedProp.isFromNestedModel) {
                val columnName = expandedProp.columnName
                val fullPath = expandedProp.fullPath

                addStatement("%LColumn.appendMissing()", columnName)
                beginControlFlow("if (item.%L != null)", expandedProp.fullPath.substringBefore("."))
                beginControlFlow("if (item.%L != null)", fullPath)

                val nonNullType = expandedProp.type.makeNotNullable()
                val typeName = nonNullType.toTypeName().toString()

                when {
                    columnType.simpleName == "StringColumn" -> {
                        addCode(
                            "    %LColumn.set(%LColumn.size() - 1, item.%L as String)\n",
                            columnName,
                            columnName,
                            fullPath
                        )
                    }

                    columnType.simpleName == "DoubleColumn" && (typeName == "java.lang.Number" || typeName == "kotlin.Number") -> {
                        addCode(
                            "    %LColumn.set(%LColumn.size() - 1, (item.%L as Number).toDouble())\n",
                            columnName,
                            columnName,
                            fullPath
                        )
                    }

                    columnType.simpleName == "LongColumn" -> {
                        addCode(
                            "    %LColumn.set(%LColumn.size() - 1, item.%L as Long)\n",
                            columnName,
                            columnName,
                            fullPath
                        )
                    }

                    columnType.simpleName == "IntColumn" -> {
                        when {
                            typeName == "kotlin.Short" || typeName == "java.lang.Short" -> {
                                addCode(
                                    "    %LColumn.set(%LColumn.size() - 1, (item.%L as Short).toInt())\n",
                                    columnName,
                                    columnName,
                                    fullPath
                                )
                            }

                            typeName == "kotlin.Byte" || typeName == "java.lang.Byte" -> {
                                addCode(
                                    "    %LColumn.set(%LColumn.size() - 1, (item.%L as Byte).toInt())\n",
                                    columnName,
                                    columnName,
                                    fullPath
                                )
                            }

                            else -> {
                                addCode(
                                    "    %LColumn.set(%LColumn.size() - 1, item.%L as Int)\n",
                                    columnName,
                                    columnName,
                                    fullPath
                                )
                            }
                        }
                    }

                    columnType.simpleName == "DoubleColumn" -> {
                        addCode(
                            "    %LColumn.set(%LColumn.size() - 1, item.%L as Double)\n",
                            columnName,
                            columnName,
                            fullPath
                        )
                    }

                    columnType.simpleName == "FloatColumn" -> {
                        addCode(
                            "    %LColumn.set(%LColumn.size() - 1, item.%L as Float)\n",
                            columnName,
                            columnName,
                            fullPath
                        )
                    }

                    columnType.simpleName == "BooleanColumn" -> {
                        addCode(
                            "    %LColumn.set(%LColumn.size() - 1, item.%L as Boolean)\n",
                            columnName,
                            columnName,
                            fullPath
                        )
                    }

                    else -> {
                        addCode(
                            "    %LColumn.set(%LColumn.size() - 1, item.%L as Any)\n",
                            columnName,
                            columnName,
                            fullPath
                        )
                    }
                }
                endControlFlow()
                endControlFlow()
            } else {
                val columnName = expandedProp.columnName
                val fullPath = expandedProp.fullPath
                val nonNullType = expandedProp.type.makeNotNullable()
                val typeName = nonNullType.toTypeName().toString()

                if (expandedProp.type.isMarkedNullable) {
                    addStatement("%LColumn.appendMissing()", columnName)
                    beginControlFlow("if (item.%L != null)", fullPath)

                    when {
                        isListType(nonNullType) -> {
                            // Serialize list to JSON
                            addCode(
                                "    %LColumn.set(%LColumn.size() - 1, item.%L.toString())\n",
                                columnName,
                                columnName,
                                fullPath
                            )
                        }

                        columnType.simpleName == "StringColumn" -> {
                            addCode(
                                "    %LColumn.set(%LColumn.size() - 1, item.%L as String)\n",
                                columnName,
                                columnName,
                                fullPath
                            )
                        }

                        columnType.simpleName == "DoubleColumn" && (typeName == "java.lang.Number" || typeName == "kotlin.Number") -> {
                            addCode(
                                "    %LColumn.set(%LColumn.size() - 1, (item.%L as Number).toDouble())\n",
                                columnName,
                                columnName,
                                fullPath
                            )
                        }

                        columnType.simpleName == "LongColumn" -> {
                            addCode(
                                "    %LColumn.set(%LColumn.size() - 1, item.%L as Long)\n",
                                columnName,
                                columnName,
                                fullPath
                            )
                        }

                        columnType.simpleName == "IntColumn" -> {
                            when {
                                typeName == "kotlin.Short" || typeName == "java.lang.Short" -> {
                                    addCode(
                                        "    %LColumn.set(%LColumn.size() - 1, (item.%L as Short).toInt())\n",
                                        columnName,
                                        columnName,
                                        fullPath
                                    )
                                }

                                typeName == "kotlin.Byte" || typeName == "java.lang.Byte" -> {
                                    addCode(
                                        "    %LColumn.set(%LColumn.size() - 1, (item.%L as Byte).toInt())\n",
                                        columnName,
                                        columnName,
                                        fullPath
                                    )
                                }

                                else -> {
                                    addCode(
                                        "    %LColumn.set(%LColumn.size() - 1, item.%L as Int)\n",
                                        columnName,
                                        columnName,
                                        fullPath
                                    )
                                }
                            }
                        }

                        columnType.simpleName == "DoubleColumn" -> {
                            addCode(
                                "    %LColumn.set(%LColumn.size() - 1, item.%L as Double)\n",
                                columnName,
                                columnName,
                                fullPath
                            )
                        }

                        columnType.simpleName == "FloatColumn" -> {
                            addCode(
                                "    %LColumn.set(%LColumn.size() - 1, item.%L as Float)\n",
                                columnName,
                                columnName,
                                fullPath
                            )
                        }

                        columnType.simpleName == "BooleanColumn" -> {
                            addCode(
                                "    %LColumn.set(%LColumn.size() - 1, item.%L as Boolean)\n",
                                columnName,
                                columnName,
                                fullPath
                            )
                        }

                        else -> {
                            addCode(
                                "    %LColumn.set(%LColumn.size() - 1, item.%L as Any)\n",
                                columnName,
                                columnName,
                                fullPath
                            )
                        }
                    }
                    endControlFlow()
                } else {
                    when {
                        isListType(nonNullType) -> {
                            // Serialize list to JSON
                            addStatement("%LColumn.append(item.%L.toString())", columnName, fullPath)
                        }

                        typeName == "java.lang.Number" || typeName == "kotlin.Number" -> {
                            addStatement("%LColumn.append((item.%L as Number).toDouble())", columnName, fullPath)
                        }

                        else -> {
                            addStatement("%LColumn.append(item.%L)", columnName, fullPath)
                        }
                    }
                }
            }
        }
    }

    private fun getTableSawColumnType(propType: KSType): ClassName {
        // Remove nullable annotation
        val nonNullType = propType.makeNotNullable()
        val typeName = nonNullType.toTypeName().toString()

        return when {
            // List types are serialized as strings
            typeName.startsWith("kotlin.collections.List") ||
                    typeName.startsWith("java.util.List") ||
                    typeName.startsWith("kotlin.Sequence") ->
                ClassName("tech.tablesaw.api", "StringColumn")

            typeName == "kotlin.String" || typeName == "java.lang.String" ->
                ClassName("tech.tablesaw.api", "StringColumn")

            typeName == "kotlin.Int" || typeName == "java.lang.Integer" ->
                ClassName("tech.tablesaw.api", "IntColumn")

            typeName == "kotlin.Double" || typeName == "java.lang.Double" ->
                ClassName("tech.tablesaw.api", "DoubleColumn")

            typeName == "kotlin.Float" || typeName == "java.lang.Float" ->
                ClassName("tech.tablesaw.api", "FloatColumn")

            typeName == "kotlin.Long" || typeName == "java.lang.Long" ->
                ClassName("tech.tablesaw.api", "LongColumn")

            typeName == "kotlin.Boolean" || typeName == "java.lang.Boolean" ->
                ClassName("tech.tablesaw.api", "BooleanColumn")

            typeName == "kotlin.Short" || typeName == "java.lang.Short" ->
                ClassName("tech.tablesaw.api", "IntColumn") // Short uses IntColumn in Tablesaw
            typeName == "kotlin.Byte" || typeName == "java.lang.Byte" ->
                ClassName("tech.tablesaw.api", "IntColumn") // Byte uses IntColumn in Tablesaw
            typeName.startsWith("java.time.LocalDate") ->
                ClassName("tech.tablesaw.api", "DateColumn")

            typeName.startsWith("java.time.LocalDateTime") ->
                ClassName("tech.tablesaw.api", "DateTimeColumn")

            typeName.startsWith("java.time.LocalTime") ->
                ClassName("tech.tablesaw.api", "TimeColumn")

            typeName.startsWith("java.math.BigDecimal") ->
                ClassName("tech.tablesaw.api", "DoubleColumn")
            // Handle java.lang.Number as double
            typeName == "java.lang.Number" || typeName == "kotlin.Number" ->
                ClassName("tech.tablesaw.api", "DoubleColumn")

            else -> ClassName("tech.tablesaw.api", "StringColumn") // fallback
        }
    }

}
